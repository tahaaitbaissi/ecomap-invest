package com.example.backend.services;

import com.example.backend.audit.Audited;
import com.example.backend.controllers.dto.HexagonMapResponse;
import com.example.backend.scoring.HexScoringConfig;
import com.example.backend.scoring.HexagonRawScoringSupport;
import com.uber.h3core.H3Core;
import com.uber.h3core.util.LatLng;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    private final H3Core h3;
    private final HexagonRawScoringSupport rawScoringSupport;
    private final HexagonScorePersistenceService hexagonScorePersistenceService;

    @Nullable
    private final StringRedisTemplate stringRedisTemplate;

    @Value("${app.hexagon.resolution:9}")
    private int h3Resolution;

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
            @Nullable StringRedisTemplate stringRedisTemplate) {
        this.h3 = h3;
        this.rawScoringSupport = rawScoringSupport;
        this.hexagonScorePersistenceService = hexagonScorePersistenceService;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Transactional(readOnly = true)
    @Audited(action = "GET_SCORE", persist = false)
    public List<HexagonMapResponse> getHexagonsInBbox(String bbox, UUID profileId) {
        Bbox b = parseBbox(bbox);
        validateBbox(b);

        List<Long> cellIndices =
                h3.polygonToCells(bboxToOuterRing(b), java.util.Collections.emptyList(), h3Resolution);
        if (cellIndices.isEmpty()) {
            return List.of();
        }
        if (cellIndices.size() > maxCells) {
            throw new IllegalArgumentException(
                    "Requested viewport spans too many H3 cells: "
                            + cellIndices.size()
                            + " (max "
                            + maxCells
                            + "). Zoom in or reduce the map area.");
        }
        List<String> h3IndexStrings =
                cellIndices.stream().map(h3::h3ToString).distinct().toList();
        if (h3IndexStrings.isEmpty()) {
            return List.of();
        }

        if (profileId == null) {
            return grayHexResponses(h3IndexStrings);
        }

        HexScoringConfig cfg = rawScoringSupport.buildConfigForProfile(profileId);
        return scoredHexResponses(profileId, cfg, h3IndexStrings);
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

    private List<HexagonMapResponse> scoredHexResponses(UUID profileId, HexScoringConfig cfg, List<String> h3IndexStrings) {
        int n = h3IndexStrings.size();
        String cacheNs = profileId.toString();
        List<String> cacheKeys =
                h3IndexStrings.stream().map(hi -> "score:" + cacheNs + ":" + hi).toList();

        List<HexagonMapResponse> withPlaceholders = new ArrayList<>(n);
        Double[] preNormalized = new Double[n];
        for (int i = 0; i < n; i++) {
            String h3Index = h3IndexStrings.get(i);
            long cell = h3.stringToH3(h3Index);
            List<HexagonMapResponse.LatLng> ring = h3.cellToBoundary(cell).stream()
                    .map(p -> new HexagonMapResponse.LatLng(p.lat, p.lng))
                    .toList();
            withPlaceholders.add(new HexagonMapResponse(h3Index, 0.0, ring));
        }

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

        List<Double> preList = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            preList.add(preNormalized[i]);
        }

        double[] minMax = minMaxOrNeutral(preList);
        double min = minMax[0];
        double max = minMax[1];
        boolean flat = minMax[2] > 0.5;

        Map<String, Double> normalizedToPersist = new HashMap<>();
        List<HexagonMapResponse> out = new ArrayList<>();
        for (int i = 0; i < withPlaceholders.size(); i++) {
            HexagonMapResponse row = withPlaceholders.get(i);
            double s = flat ? 50.0 : 100.0 * (preList.get(i) - min) / (max - min);
            normalizedToPersist.put(row.h3Index(), s);
            out.add(new HexagonMapResponse(row.h3Index(), s, row.boundary()));
        }

        try {
            hexagonScorePersistenceService.upsertViewportScores(profileId, normalizedToPersist);
        } catch (Exception e) {
            log.warn("hexagon_score persistence failed: {}", e.getMessage());
        }

        return out;
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

    /** [min, max, flatFlag] with flatFlag 1.0 if min==max */
    public static double[] minMaxOrNeutral(List<Double> preNormalized) {
        if (preNormalized.isEmpty()) {
            return new double[] {0, 0, 1.0};
        }
        double min = preNormalized.stream().mapToDouble(Double::doubleValue).min().orElse(0);
        double max = preNormalized.stream().mapToDouble(Double::doubleValue).max().orElse(0);
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

    private List<LatLng> bboxToOuterRing(Bbox b) {
        List<LatLng> ring = new ArrayList<>();
        ring.add(new LatLng(b.swLat, b.swLng));
        ring.add(new LatLng(b.swLat, b.neLng));
        ring.add(new LatLng(b.neLat, b.neLng));
        ring.add(new LatLng(b.neLat, b.swLng));
        return ring;
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

    private record Bbox(double swLng, double swLat, double neLng, double neLat) {}
}
