package com.example.backend.services;

import com.example.backend.audit.Audited;
import com.example.backend.controllers.dto.HexagonMapResponse;
import com.example.backend.repositories.H3HexagonRepository;
import com.example.backend.scoring.HexScoringConfig;
import com.example.backend.scoring.HexagonRawScoringSupport;
import com.example.backend.scoring.RawScoreRefBounds;
import com.uber.h3core.H3Core;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class HexagonScoringService {

    public static final String DEFAULT_PROFILE_CACHE_NS = "__none__";
    private static final String RAW_CACHE_PREFIX = "score:v3:";

    private final H3Core h3;
    private final HexagonRawScoringSupport rawScoringSupport;
    private final HexagonScorePersistenceService hexagonScorePersistenceService;
    private final H3HexagonRepository h3HexagonRepository;

    @Nullable
    private final StringRedisTemplate stringRedisTemplate;

    private final ProfileScoreScaleService profileScoreScaleService;

    private final com.example.backend.services.admin.ScoreCacheVersionService scoreCacheVersionService;

    @Value("${app.hexagon.max-cells:2000}")
    private int maxCells;

    @Value("${app.hexagon.max-bbox-deg:0.5}")
    private double maxBboxDeg;

    @Value("${app.hexagon.cache.enabled:true}")
    private boolean cacheEnabled;

    @Value("${app.hexagon.cache.ttl-seconds:3600}")
    private int cacheTtlSeconds;

    @Autowired
    public HexagonScoringService(
            H3Core h3,
            HexagonRawScoringSupport rawScoringSupport,
            HexagonScorePersistenceService hexagonScorePersistenceService,
            H3HexagonRepository h3HexagonRepository,
            @Nullable StringRedisTemplate stringRedisTemplate,
            ProfileScoreScaleService profileScoreScaleService,
            com.example.backend.services.admin.ScoreCacheVersionService scoreCacheVersionService) {
        this.h3 = h3;
        this.rawScoringSupport = rawScoringSupport;
        this.hexagonScorePersistenceService = hexagonScorePersistenceService;
        this.h3HexagonRepository = h3HexagonRepository;
        this.stringRedisTemplate = stringRedisTemplate;
        this.profileScoreScaleService = profileScoreScaleService;
        this.scoreCacheVersionService = scoreCacheVersionService;
    }

    @Transactional(readOnly = true)
    @Audited(action = "GET_SCORE", persist = false)
    public List<HexagonMapResponse> getHexagonsInBbox(String bbox, UUID profileId) {
        return getHexagonsInBbox(bbox, profileId, null);
    }

    @Transactional(readOnly = true)
    @Audited(action = "GET_SCORE", persist = false)
    public List<HexagonMapResponse> getHexagonsInBbox(String bbox, UUID profileId, Integer h3Resolution) {
        long startedAt = System.nanoTime();
        Bbox b = parseBbox(bbox);
        validateBbox(b);

        int targetRes = h3Resolution == null ? 9 : h3Resolution;
        if (targetRes < 7 || targetRes > 9) {
            throw new IllegalArgumentException("h3Resolution must be between 7 and 9 inclusive");
        }

        // Use PostGIS + persisted grid so zoomed-out viewports stay bounded (polygonToCells at res 9
        // can exceed max-cells for modest degree spans).
        List<String> h3IndexStrings =
                h3HexagonRepository
                        .findH3IndicesIntersectingBbox(b.swLng, b.swLat, b.neLng, b.neLat)
                        .stream()
                        .distinct()
                        .toList();
        long queryMs = elapsedMs(startedAt);
        if (h3IndexStrings.isEmpty()) {
            log.info("Hex viewport loaded 0 cells in {}ms (query={}ms)", elapsedMs(startedAt), queryMs);
            return List.of();
        }
        if (h3IndexStrings.size() > maxCells) {
            throw new IllegalArgumentException(
                    "Requested viewport intersects too many grid cells: "
                            + h3IndexStrings.size()
                            + " (max "
                            + maxCells
                            + "). Zoom in or reduce the map area.");
        }

        if (targetRes == 9) {
            List<HexagonMapResponse> out;
            if (profileId == null) {
                out = grayHexResponses(h3IndexStrings);
            } else {
                HexScoringConfig cfg = rawScoringSupport.buildConfigForProfile(profileId);
                out = scoredHexResponses(profileId, cfg, h3IndexStrings);
            }
            log.info(
                    "Hex viewport loaded {} cells at res {} in {}ms (query={}ms, profile={})",
                    out.size(),
                    targetRes,
                    elapsedMs(startedAt),
                    queryMs,
                    profileId != null);
            return out;
        }

        List<HexagonMapResponse> out;
        if (profileId == null) {
            out = grayHexResponsesAggregated(h3IndexStrings, targetRes);
        } else {
            HexScoringConfig cfg = rawScoringSupport.buildConfigForProfile(profileId);
            out = scoredHexResponsesAggregated(profileId, cfg, h3IndexStrings, targetRes);
        }
        log.info(
                "Hex viewport rolled {} child cells into {} parent cells at res {} in {}ms (query={}ms, profile={})",
                h3IndexStrings.size(),
                out.size(),
                targetRes,
                elapsedMs(startedAt),
                queryMs,
                profileId != null);
        return out;
    }

    private List<HexagonMapResponse> grayHexResponses(List<String> h3IndexStrings) {
        List<HexagonMapResponse> out = new ArrayList<>();
        for (String h3Index : h3IndexStrings) {
            long cell = h3.stringToH3(h3Index);
            List<HexagonMapResponse.LatLng> ring = h3.cellToBoundary(cell).stream()
                    .map(p -> new HexagonMapResponse.LatLng(p.lat, p.lng))
                    .toList();
            out.add(new HexagonMapResponse(h3Index, null, ring));
        }
        return out;
    }

    /** Gray mode: roll res-9 children up to parent cells (fewer polygons when zoomed out). */
    private List<HexagonMapResponse> grayHexResponsesAggregated(List<String> childIndices, int targetRes) {
        List<String> parents = distinctParentIndices(childIndices, targetRes);
        return grayHexResponses(parents);
    }

    private List<String> distinctParentIndices(List<String> childIndices, int targetRes) {
        LinkedHashSet<String> parents = new LinkedHashSet<>();
        for (String child : childIndices) {
            long c = h3.stringToH3(child);
            long p = h3.cellToParent(c, targetRes);
            parents.add(h3.h3ToString(p));
        }
        return new ArrayList<>(parents);
    }

    /**
     * Profile mode at coarser H3 resolution: average child raw scores per parent, normalize across
     * parents in the response. Does not persist parent scores (parents are not rows in h3_hexagon).
     */
    private List<HexagonMapResponse> scoredHexResponsesAggregated(
            UUID profileId, HexScoringConfig cfg, List<String> childIndices, int targetRes) {
        int n = childIndices.size();
        Double[] preNormalized = resolveChildRawScores(profileId, cfg, childIndices);

        LinkedHashMap<String, List<Integer>> parentToChildIdx = new LinkedHashMap<>();
        for (int i = 0; i < n; i++) {
            long c = h3.stringToH3(childIndices.get(i));
            String parent = h3.h3ToString(h3.cellToParent(c, targetRes));
            parentToChildIdx.computeIfAbsent(parent, k -> new ArrayList<>()).add(i);
        }

        List<String> parentKeys = new ArrayList<>(parentToChildIdx.keySet());
        List<Double> parentRaws = new ArrayList<>();
        for (String parent : parentKeys) {
            List<Integer> idxs = parentToChildIdx.get(parent);
            double sum = 0;
            int cnt = 0;
            for (int i : idxs) {
                Double r = preNormalized[i];
                if (r != null && Double.isFinite(r)) {
                    sum += r;
                    cnt++;
                }
            }
            parentRaws.add(cnt == 0 ? 0.0 : sum / cnt);
        }

        int nParents = parentKeys.size();
        RawScoreRefBounds ref = profileScoreScaleService.resolveRefBounds(profileId, cfg);

        List<HexagonMapResponse> out = new ArrayList<>();
        for (int pi = 0; pi < parentKeys.size(); pi++) {
            String parent = parentKeys.get(pi);
            long cell = h3.stringToH3(parent);
            List<HexagonMapResponse.LatLng> ring = h3.cellToBoundary(cell).stream()
                    .map(p -> new HexagonMapResponse.LatLng(p.lat, p.lng))
                    .toList();
            double raw = parentRaws.get(pi);
            Double s = ProfileScoreScaleService.toHeatmapDisplayScore(raw, ref, nParents);
            out.add(new HexagonMapResponse(parent, s, ring));
        }
        if (nParents >= 2 && ref.flat()) {
            log.debug(
                    "Hex viewport (aggregated): suppressed uniform grid-scale score — {} parent cells (global raw min=max)",
                    nParents);
        }
        return out;
    }

    private List<HexagonMapResponse> scoredHexResponses(UUID profileId, HexScoringConfig cfg, List<String> h3IndexStrings) {
        int n = h3IndexStrings.size();

        List<HexagonMapResponse> withPlaceholders = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            String h3Index = h3IndexStrings.get(i);
            long cell = h3.stringToH3(h3Index);
            List<HexagonMapResponse.LatLng> ring = h3.cellToBoundary(cell).stream()
                    .map(p -> new HexagonMapResponse.LatLng(p.lat, p.lng))
                    .toList();
            withPlaceholders.add(new HexagonMapResponse(h3Index, 0.0, ring));
        }

        Double[] preNormalized = resolveChildRawScores(profileId, cfg, h3IndexStrings);

        List<Double> preList = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            preList.add(preNormalized[i]);
        }

        RawScoreRefBounds ref = profileScoreScaleService.resolveRefBounds(profileId, cfg);

        Map<String, Double> normalizedToPersist = new HashMap<>();
        List<HexagonMapResponse> out = new ArrayList<>();
        for (int i = 0; i < withPlaceholders.size(); i++) {
            HexagonMapResponse row = withPlaceholders.get(i);
            Double raw = preList.get(i);
            double rawVal = raw != null && Double.isFinite(raw) ? raw : 0.0;
            Double s = ProfileScoreScaleService.toHeatmapDisplayScore(rawVal, ref, n);
            if (s != null) {
                normalizedToPersist.put(row.h3Index(), s);
            }
            out.add(new HexagonMapResponse(row.h3Index(), s, row.boundary()));
        }
        if (n >= 2 && ref.flat()) {
            log.debug(
                    "Hex viewport: suppressed uniform grid-scale score — {} cells (global raw min=max)",
                    n);
        }

        try {
            hexagonScorePersistenceService.upsertViewportScores(profileId, normalizedToPersist);
        } catch (Exception e) {
            log.warn("hexagon_score async persistence scheduling failed: {}", e.getMessage());
        }

        return out;
    }

    /** Per-child raw scores (Redis cache + compute); updates Redis for cache misses. */
    private Double[] resolveChildRawScores(UUID profileId, HexScoringConfig cfg, List<String> h3IndexStrings) {
        int n = h3IndexStrings.size();
        String cacheNs = profileId.toString();
        long poiV = scoreCacheVersionService.getPoiVersion();
        long demoV = scoreCacheVersionService.getDemoVersion();
        String cachePrefix = RAW_CACHE_PREFIX + "p" + poiV + "d" + demoV + ":";
        List<String> cacheKeys =
                h3IndexStrings.stream().map(hi -> cachePrefix + cacheNs + ":" + hi).toList();

        Double[] preNormalized = new Double[n];
        boolean[] fromRedis = new boolean[n];
        if (stringRedisTemplate != null && cacheEnabled) {
            try {
                List<String> cached = stringRedisTemplate.opsForValue().multiGet(cacheKeys);
                for (int i = 0; i < n; i++) {
                    if (cached == null) {
                        break;
                    }
                    String c = i < cached.size() ? cached.get(i) : null;
                    if (c != null && !c.isBlank()) {
                        try {
                            preNormalized[i] = Double.parseDouble(c);
                            fromRedis[i] = true;
                        } catch (NumberFormatException e) {
                            preNormalized[i] = null;
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Redis MGET for hex score cache failed: {}", e.getMessage());
            }
        }

        for (int i = 0; i < n; i++) {
            if (preNormalized[i] == null) {
                String h3Index = h3IndexStrings.get(i);
                preNormalized[i] = rawScoringSupport.computeRaw(h3Index, cfg);
            }
        }

        storeNewRawScoresInCachePipelined(fromRedis, cacheKeys, preNormalized, n);
        return preNormalized;
    }

    private void storeNewRawScoresInCachePipelined(
            boolean[] fromRedis, List<String> cacheKeys, Double[] preNormalized, int n) {
        if (stringRedisTemplate == null || !cacheEnabled || cacheTtlSeconds <= 0) {
            return;
        }
        try {
            RedisSerializer<String> ser = stringRedisTemplate.getStringSerializer();
            stringRedisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                for (int i = 0; i < n; i++) {
                    if (fromRedis[i] || preNormalized[i] == null) {
                        continue;
                    }
                    byte[] kb = ser.serialize(cacheKeys.get(i));
                    byte[] vb = ser.serialize(String.valueOf(preNormalized[i]));
                    if (kb != null && vb != null) {
                        connection.setEx(kb, cacheTtlSeconds, vb);
                    }
                }
                return null;
            });
        } catch (DataAccessException e) {
            log.warn("Redis pipelined SETEX for hex score cache failed: {}", e.getMessage());
        }
    }

    /** [min, max, flatFlag] with flatFlag 1.0 if min==max (ignores null / non-finite values). */
    public static double[] minMaxOrNeutral(List<Double> preNormalized) {
        double[] mm =
                preNormalized.stream()
                        .filter(Objects::nonNull)
                        .filter(Double::isFinite)
                        .mapToDouble(Double::doubleValue)
                        .toArray();
        if (mm.length == 0) {
            return new double[] {0, 0, 1.0};
        }
        double min = mm[0];
        double max = mm[0];
        for (int i = 1; i < mm.length; i++) {
            min = Math.min(min, mm[i]);
            max = Math.max(max, mm[i]);
        }
        if (min == max) {
            return new double[] {min, max, 1.0};
        }
        return new double[] {min, max, 0.0};
    }

    private void validateBbox(Bbox b) {
        if (!isFiniteBbox(b)) {
            throw new IllegalArgumentException("bbox values must be finite");
        }
        if (b.neLat < b.swLat || b.neLng < b.swLng) {
            throw new IllegalArgumentException("bbox must have neLat >= swLat and neLng >= swLng");
        }
        if (b.neLat - b.swLat > maxBboxDeg || b.neLng - b.swLng > maxBboxDeg) {
            throw new IllegalArgumentException(
                    "bbox span may not exceed " + maxBboxDeg + " degrees in latitude and longitude");
        }
    }

    private static boolean isFiniteBbox(Bbox b) {
        return Double.isFinite(b.swLng)
                && Double.isFinite(b.swLat)
                && Double.isFinite(b.neLng)
                && Double.isFinite(b.neLat);
    }

    private static Bbox parseBbox(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("bbox is required");
        }
        String[] parts = raw.split(",");
        if (parts.length != 4) {
            throw new IllegalArgumentException("bbox must be 4 comma-separated values: swLng,swLat,neLng,neLat");
        }
        try {
            return new Bbox(
                    Double.parseDouble(parts[0].trim()),
                    Double.parseDouble(parts[1].trim()),
                    Double.parseDouble(parts[2].trim()),
                    Double.parseDouble(parts[3].trim()));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("bbox contains invalid numeric values", e);
        }
    }

    private static long elapsedMs(long startedAtNanos) {
        return (System.nanoTime() - startedAtNanos) / 1_000_000;
    }

    private record Bbox(double swLng, double swLat, double neLng, double neLat) {}
}
