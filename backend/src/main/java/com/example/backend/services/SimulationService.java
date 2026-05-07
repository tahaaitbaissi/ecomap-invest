package com.example.backend.services;

import com.example.backend.audit.Audited;
import com.example.backend.controllers.dto.HexagonMapResponse;
import com.example.backend.controllers.dto.SimulateResponse;
import com.example.backend.controllers.dto.SimulationImpactType;
import com.example.backend.repositories.H3HexagonRepository;
import com.example.backend.scoring.HexScoringConfig;
import com.example.backend.scoring.HexagonRawScoringSupport;
import com.example.backend.services.profile.DynamicProfileService;
import com.example.backend.services.profile.ProfileTagCatalog;
import com.uber.h3core.H3Core;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SimulationService {

    private static final int K_RING = 2;
    private static final int SCAN_COUNT = 200;
    private static final int DELETE_BATCH = 500;

    private final H3Core h3;
    private final HexagonRawScoringSupport rawScoringSupport;
    private final DynamicProfileService dynamicProfileService;
    private final ProfileTagCatalog profileTagCatalog;
    private final StringRedisTemplate stringRedisTemplate;
    private final H3HexagonRepository h3HexagonRepository;
    private final ProfileScoreScaleService profileScoreScaleService;

    @Value("${app.hexagon.resolution:9}")
    private int h3Resolution;

    @Value("${app.simulation.ttl-seconds:600}")
    private int simulationTtlSeconds;

    @Value("${app.hexagon.max-bbox-deg:0.5}")
    private double maxBboxDeg;

    @Audited(action = "SIMULATE")
    public SimulateResponse simulateImpact(
            double lat,
            double lng,
            SimulationImpactType type,
            String tag,
            UUID profileId,
            String sessionId,
            String bbox,
            String userEmail) {
        if (userEmail == null || userEmail.isBlank()) {
            throw new IllegalArgumentException("userEmail must be provided");
        }
        Bbox viewport = parseBbox(bbox);
        validateBbox(viewport);
        dynamicProfileService.getOwnedActiveEntity(userEmail, profileId);

        HexScoringConfig cfg = rawScoringSupport.buildConfigForProfile(profileId);
        String resolvedTag = profileTagCatalog
                .canonicalTag(tag.trim())
                .orElseThrow(() -> new IllegalArgumentException("unsupported tag for simulation: " + tag));
        double baseWeight =
                switch (type) {
                    case DRIVER ->
                            rawScoringSupport
                                    .findDriverWeight(cfg, resolvedTag)
                                    .orElseThrow(
                                            () ->
                                                    new IllegalArgumentException(
                                                            "tag not in profile for DRIVER: " + resolvedTag));
                    case COMPETITOR ->
                            rawScoringSupport
                                    .findCompetitorWeight(cfg, resolvedTag)
                                    .orElseThrow(
                                            () ->
                                                    new IllegalArgumentException(
                                                            "tag not in profile for COMPETITOR: " + resolvedTag));
                };

        long center = h3.latLngToCell(lat, lng, h3Resolution);
        List<Long> disk = h3.gridDisk(center, K_RING);
        List<String> h3Indices = new ArrayList<>(disk.size());
        for (Long cell : disk) {
            h3Indices.add(h3.h3ToString(cell));
        }

        Map<String, Double> clickDeltas = new LinkedHashMap<>();
        for (int i = 0; i < h3Indices.size(); i++) {
            String h3Index = h3Indices.get(i);
            long cell = disk.get(i);
            int ring = (int) h3.gridDistance(center, cell);
            double attenuation = ringAttenuation(ring);
            double delta = attenuation * baseWeight * (type == SimulationImpactType.DRIVER ? 1.0 : -1.0);
            clickDeltas.merge(h3Index, delta, Double::sum);
        }

        Map<String, Double> sessionDeltas = readSessionDeltas(sessionId, h3Indices);
        for (Map.Entry<String, Double> e : clickDeltas.entrySet()) {
            sessionDeltas.merge(e.getKey(), e.getValue(), Double::sum);
        }
        writeSimulationDeltas(sessionId, sessionDeltas);

        List<String> viewportIndices = h3HexagonRepository
                .findH3IndicesIntersectingBbox(
                        viewport.swLng,
                        viewport.swLat,
                        viewport.neLng,
                        viewport.neLat)
                .stream()
                .distinct()
                .toList();
        Map<String, Double> viewportDeltas = readSessionDeltas(sessionId, viewportIndices);
        for (Map.Entry<String, Double> e : sessionDeltas.entrySet()) {
            viewportDeltas.put(e.getKey(), e.getValue());
        }

        List<Double> adjustedRaw = new ArrayList<>(viewportIndices.size());
        for (String h3Index : viewportIndices) {
            double raw = rawScoringSupport.computeRaw(h3Index, cfg) + viewportDeltas.getOrDefault(h3Index, 0.0);
            adjustedRaw.add(raw);
        }

        var ref = profileScoreScaleService.resolveRefBounds(profileId, cfg);

        List<HexagonMapResponse> affected = new ArrayList<>();
        Map<String, Double> redisWrites = new LinkedHashMap<>();
        for (int i = 0; i < viewportIndices.size(); i++) {
            String h3Index = viewportIndices.get(i);
            double r = adjustedRaw.get(i);
            double normalized = ProfileScoreScaleService.toSimAdjustedDisplay(r, ref);
            List<HexagonMapResponse.LatLng> boundary =
                    rawScoringSupport.boundaryPoints(h3Index).stream()
                            .map(p -> new HexagonMapResponse.LatLng(p.lat(), p.lng()))
                            .toList();
            affected.add(new HexagonMapResponse(h3Index, normalized, boundary));
            redisWrites.put(simRedisKey(sessionId, h3Index), normalized);
        }

        writeSimulationScores(redisWrites);
        return new SimulateResponse(affected);
    }

    @Audited(action = "CLEAR_SIMULATION_SESSION")
    public void deleteSimulationSession(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("sessionId must be provided");
        }
        String scorePattern = "sim_score:" + sessionId.trim() + ":*";
        String deltaPattern = "sim_delta:" + sessionId.trim() + ":*";
        long deleted = deletePattern(scorePattern) + deletePattern(deltaPattern);
        log.debug("Simulation session {} cleared: {} key(s)", sessionId, deleted);
    }

    private long deletePattern(String pattern) {
        ScanOptions options = ScanOptions.scanOptions().match(pattern).count(SCAN_COUNT).build();
        long deleted = 0;
        List<String> batch = new ArrayList<>();
        try (Cursor<String> cursor = stringRedisTemplate.scan(options)) {
            while (cursor.hasNext()) {
                batch.add(cursor.next());
                if (batch.size() >= DELETE_BATCH) {
                    Long n = stringRedisTemplate.delete(batch);
                    deleted += n != null ? n : 0;
                    batch.clear();
                }
            }
            if (!batch.isEmpty()) {
                Long n = stringRedisTemplate.delete(batch);
                deleted += n != null ? n : 0;
            }
        } catch (Exception e) {
            log.warn("Redis SCAN/delete failed for pattern {}: {}", pattern, e.getMessage());
        }
        return deleted;
    }

    private static double ringAttenuation(int ring) {
        return switch (ring) {
            case 0 -> 1.0;
            case 1 -> 0.6;
            case 2 -> 0.3;
            default -> 0.0;
        };
    }

    private static String simRedisKey(String sessionId, String h3Index) {
        return "sim_score:" + sessionId + ":" + h3Index;
    }

    private static String simDeltaRedisKey(String sessionId, String h3Index) {
        return "sim_delta:" + sessionId + ":" + h3Index;
    }

    private Map<String, Double> readSessionDeltas(String sessionId, List<String> h3Indices) {
        Map<String, Double> out = new LinkedHashMap<>();
        if (h3Indices.isEmpty()) {
            return out;
        }
        List<String> keys = h3Indices.stream().map(h -> simDeltaRedisKey(sessionId, h)).toList();
        try {
            List<String> values = stringRedisTemplate.opsForValue().multiGet(keys);
            if (values == null) {
                return out;
            }
            for (int i = 0; i < h3Indices.size(); i++) {
                String value = i < values.size() ? values.get(i) : null;
                if (value == null || value.isBlank()) {
                    continue;
                }
                try {
                    out.put(h3Indices.get(i), Double.parseDouble(value));
                } catch (NumberFormatException e) {
                    log.warn("Ignoring invalid simulation delta for {}: {}", h3Indices.get(i), value);
                }
            }
        } catch (Exception e) {
            log.warn("Redis MGET for simulation deltas failed: {}", e.getMessage());
        }
        return out;
    }

    private void writeSimulationDeltas(String sessionId, Map<String, Double> h3ToDelta) {
        if (h3ToDelta.isEmpty() || simulationTtlSeconds <= 0) {
            return;
        }
        try {
            RedisSerializer<String> ser = stringRedisTemplate.getStringSerializer();
            stringRedisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                for (Map.Entry<String, Double> e : h3ToDelta.entrySet()) {
                    byte[] kb = ser.serialize(simDeltaRedisKey(sessionId, e.getKey()));
                    byte[] vb = ser.serialize(String.valueOf(e.getValue()));
                    if (kb != null && vb != null) {
                        connection.setEx(kb, simulationTtlSeconds, vb);
                    }
                }
                return null;
            });
        } catch (DataAccessException e) {
            log.warn("Redis SETEX for simulation deltas failed: {}", e.getMessage());
        }
    }

    private void writeSimulationScores(Map<String, Double> keyToScore) {
        if (keyToScore.isEmpty()) {
            return;
        }
        if (simulationTtlSeconds <= 0) {
            return;
        }
        try {
            RedisSerializer<String> ser = stringRedisTemplate.getStringSerializer();
            stringRedisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                for (Map.Entry<String, Double> e : keyToScore.entrySet()) {
                    byte[] kb = ser.serialize(e.getKey());
                    byte[] vb = ser.serialize(String.valueOf(e.getValue()));
                    if (kb != null && vb != null) {
                        connection.setEx(kb, simulationTtlSeconds, vb);
                    }
                }
                return null;
            });
        } catch (DataAccessException e) {
            log.warn("Redis SETEX for simulation scores failed: {}", e.getMessage());
        }
    }

    private void validateBbox(Bbox b) {
        if (!Double.isFinite(b.swLng)
                || !Double.isFinite(b.swLat)
                || !Double.isFinite(b.neLng)
                || !Double.isFinite(b.neLat)) {
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
