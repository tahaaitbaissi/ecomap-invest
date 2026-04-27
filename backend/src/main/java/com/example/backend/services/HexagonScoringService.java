package com.example.backend.services;

import com.example.backend.audit.Audited;
import com.example.backend.controllers.dto.HexagonMapResponse;
import com.example.backend.entities.Demographics;
import com.example.backend.entities.DynamicProfile;
import com.example.backend.repositories.DemographicsRepository;
import com.example.backend.repositories.DynamicProfileRepository;
import com.example.backend.repositories.PoiRepository;
import com.example.backend.scoring.SaturationFormula;
import com.fasterxml.jackson.databind.JsonNode;
import com.uber.h3core.H3Core;
import com.uber.h3core.util.LatLng;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class HexagonScoringService {

    public static final String DEFAULT_PROFILE_CACHE_NS = "__none__";

    private final H3Core h3;
    private final PoiRepository poiRepository;
    private final DynamicProfileRepository dynamicProfileRepository;
    private final DemographicsRepository demographicsRepository;
    @Nullable
    private final StringRedisTemplate stringRedisTemplate;

    @Value("${app.poi.driver-categories:category=school,category=university,category=office}")
    private String driverCategoriesConfig;

    @Value("${app.poi.radius-drivers-km:2.0}")
    private double radiusDriversKm;

    @Value("${app.poi.radius-competitors-km:1.0}")
    private double radiusCompetitorsKm;

    @Value("${app.poi.max-density-count:50}")
    private int maxDensityCount;

    @Value("${app.hexagon.resolution:9}")
    private int h3Resolution;

    @Value("${app.hexagon.max-cells:2000}")
    private int maxCells;

    @Value("${app.hexagon.max-bbox-deg:0.5}")
    private double maxBboxDeg;

    @Value("${app.hexagon.default-competitor-tags:}")
    private String defaultCompetitorTags;

    @Value("${app.hexagon.demographic-density-cap:20000.0}")
    private double demographicDensityCap;

    @Value("${app.hexagon.demographic-blend:0.0}")
    private double demographicBlend;

    @Value("${app.hexagon.cache.enabled:true}")
    private boolean cacheEnabled;

    @Value("${app.hexagon.cache.ttl-seconds:3600}")
    private int cacheTtlSeconds;

    @Autowired
    public HexagonScoringService(
            H3Core h3,
            PoiRepository poiRepository,
            DynamicProfileRepository dynamicProfileRepository,
            DemographicsRepository demographicsRepository,
            @Nullable StringRedisTemplate stringRedisTemplate) {
        this.h3 = h3;
        this.poiRepository = poiRepository;
        this.dynamicProfileRepository = dynamicProfileRepository;
        this.demographicsRepository = demographicsRepository;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Transactional(readOnly = true)
    @Audited(action = "GET_SCORE", persist = false)
    public List<HexagonMapResponse> getHexagonsInBbox(String bbox, UUID profileId) {
        Bbox b = parseBbox(bbox);
        validateBbox(b);
        // Resolve profile first so invalid profileId fails before expensive H3 work
        ScoringConfig cfg = buildScoringConfig(profileId);

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
        List<String> h3IndexStrings = cellIndices.stream()
                .map(h3::h3ToString)
                .distinct()
                .toList();
        if (h3IndexStrings.isEmpty()) {
            return List.of();
        }

        double driverRadiusM = radiusDriversKm * 1000.0;
        double competitorRadiusM = radiusCompetitorsKm * 1000.0;

        int n = h3IndexStrings.size();
        String cacheNs = profileId == null ? DEFAULT_PROFILE_CACHE_NS : profileId.toString();
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
                preNormalized[i] = computeCellRaw(
                        h3Index, cfg, driverRadiusM, competitorRadiusM);
            }
        }

        storeNewRawScoresInCacheIfNeeded(fromRedis, cacheKeys, preNormalized, n);

        List<Double> preList = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            preList.add(preNormalized[i]);
        }

        double[] minMax = minMaxOrNeutral(preList);
        double min = minMax[0];
        double max = minMax[1];
        boolean flat = minMax[2] > 0.5; // 1.0 = all same

        List<HexagonMapResponse> out = new ArrayList<>();
        for (int i = 0; i < withPlaceholders.size(); i++) {
            HexagonMapResponse row = withPlaceholders.get(i);
            double s = flat ? 50.0 : 100.0 * (preList.get(i) - min) / (max - min);
            out.add(new HexagonMapResponse(row.h3Index(), s, row.boundary()));
        }
        return out;
    }

    private void storeNewRawScoresInCacheIfNeeded(
            boolean[] fromRedis, List<String> cacheKeys, Double[] preNormalized, int n) {
        if (stringRedisTemplate == null || !cacheEnabled || cacheTtlSeconds <= 0) {
            return;
        }
        try {
            for (int i = 0; i < n; i++) {
                if (fromRedis[i] || preNormalized[i] == null) {
                    continue;
                }
                String key = cacheKeys.get(i);
                String val = String.valueOf(preNormalized[i]);
                stringRedisTemplate
                        .opsForValue()
                        .set(key, val, cacheTtlSeconds, TimeUnit.SECONDS);
            }
        } catch (Exception e) {
            log.warn("Redis SET for hex score cache failed: {}", e.getMessage());
        }
    }

    private double computeCellRaw(
            String h3Index, ScoringConfig cfg, double driverRadiusM, double competitorRadiusM) {
        long cell = h3.stringToH3(h3Index);
        LatLng c = h3.cellToLatLng(cell);
        double lat = c.lat;
        double lng = c.lng;
        int driversI = toBoundedInt(weightedCount(cfg.driverTags, lat, lng, driverRadiusM));
        int competitorsI = toBoundedInt(weightedCount(cfg.competitorTags, lat, lng, competitorRadiusM));
        double density01 = densityForCell(h3Index, lat, lng, driverRadiusM, cfg);
        return SaturationFormula.apply(driversI, competitorsI, density01);
    }

    /** [min, max, flatFlag] with flatFlag 1.0 if min==max */
    static double[] minMaxOrNeutral(List<Double> preNormalized) {
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

    private double densityForCell(String h3Index, double lat, double lng, double driverRadiusM, ScoringConfig cfg) {
        long nearby = poiRepository.countAllNearby(lat, lng, driverRadiusM);
        double proxy = Math.min((double) nearby / Math.max(1, maxDensityCount), 1.0);

        if (demographicBlend <= 0) {
            return proxy;
        }
        Optional<Demographics> d = cfg.useDemographics ? demographicsRepository.findById(h3Index) : Optional.empty();
        if (d.isEmpty() || d.get().getPopulationDensity() == null) {
            return proxy;
        }
        double fromDemo = Math.min(d.get().getPopulationDensity() / Math.max(1.0, demographicDensityCap), 1.0);
        return (1.0 - demographicBlend) * proxy + demographicBlend * fromDemo;
    }

    private int toBoundedInt(double w) {
        if (w <= 0) {
            return 0;
        }
        if (w >= 10_000_000) {
            return 10_000_000;
        }
        return (int) Math.round(w);
    }

    private double weightedCount(List<TagWeight> tags, double lat, double lng, double radiusMeters) {
        double s = 0.0;
        for (TagWeight t : tags) {
            s += t.weight() * poiRepository.countByTypeTagAndNearby(t.tag(), lat, lng, radiusMeters);
        }
        return s;
    }

    private ScoringConfig buildScoringConfig(UUID profileId) {
        if (profileId == null) {
            return defaultConfig();
        }
        DynamicProfile p = dynamicProfileRepository
                .findById(profileId)
                .orElseThrow(
                        () -> new java.util.NoSuchElementException("Unknown profile: " + profileId));
        List<TagWeight> drivers = parseTagWeights(p.getDriversConfig());
        List<TagWeight> comp = parseTagWeights(p.getCompetitorsConfig());
        if (drivers.isEmpty() && comp.isEmpty()) {
            return defaultConfig();
        }
        return new ScoringConfig(drivers.isEmpty() ? defaultDrivers() : drivers, comp.isEmpty() ? defaultCompetitors() : comp, true);
    }

    private ScoringConfig defaultConfig() {
        return new ScoringConfig(defaultDrivers(), defaultCompetitors(), demographicBlend > 0);
    }

    private List<TagWeight> defaultDrivers() {
        return Stream.of(driverCategoriesConfig.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(s -> new TagWeight(s, 1.0))
                .toList();
    }

    private List<TagWeight> defaultCompetitors() {
        if (defaultCompetitorTags == null || defaultCompetitorTags.isBlank()) {
            return List.of(new TagWeight("amenity=restaurant", 1.0));
        }
        return Stream.of(defaultCompetitorTags.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(s -> new TagWeight(s, 1.0))
                .toList();
    }

    private static List<TagWeight> parseTagWeights(JsonNode root) {
        if (root == null || !root.isArray() || root.isEmpty()) {
            return List.of();
        }
        List<TagWeight> out = new ArrayList<>();
        for (JsonNode n : root) {
            if (n == null || !n.isObject()) {
                continue;
            }
            String tag = n.hasNonNull("tag") ? n.get("tag").asText() : "";
            if (tag.isBlank()) {
                continue;
            }
            double w = 1.0;
            if (n.has("weight") && n.get("weight").isNumber()) {
                w = n.get("weight").asDouble(1.0);
            } else if (n.has("weight") && n.get("weight").isTextual()) {
                try {
                    w = Double.parseDouble(n.get("weight").asText());
                } catch (NumberFormatException e) {
                    w = 1.0;
                }
            }
            out.add(new TagWeight(tag, w));
        }
        return out;
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
        // Counter-clockwise, closed quadrilateral: SW, SE, NE, NW
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

    private record ScoringConfig(
            List<TagWeight> driverTags, List<TagWeight> competitorTags, boolean useDemographics) {}

    private record TagWeight(String tag, double weight) {}
}
