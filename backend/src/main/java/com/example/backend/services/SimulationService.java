package com.example.backend.services;

import com.example.backend.audit.Audited;
import com.example.backend.controllers.dto.HexagonMapResponse;
import com.example.backend.controllers.dto.SimulateResponse;
import com.example.backend.controllers.dto.SimulationImpactType;
import com.example.backend.entities.DynamicProfile;
import com.example.backend.entities.User;
import com.example.backend.repositories.DynamicProfileRepository;
import com.example.backend.scoring.HexScoringConfig;
import com.example.backend.scoring.HexagonRawScoringSupport;
import com.uber.h3core.H3Core;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
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
import org.springframework.security.access.AccessDeniedException;
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
    private final DynamicProfileRepository dynamicProfileRepository;
    private final UserService userService;
    private final StringRedisTemplate stringRedisTemplate;

    @Value("${app.hexagon.resolution:9}")
    private int h3Resolution;

    @Value("${app.simulation.ttl-seconds:600}")
    private int simulationTtlSeconds;

    @Audited(action = "SIMULATE")
    public SimulateResponse simulateImpact(
            double lat,
            double lng,
            SimulationImpactType type,
            String tag,
            UUID profileId,
            String sessionId,
            String userEmail) {
        if (userEmail == null || userEmail.isBlank()) {
            throw new IllegalArgumentException("userEmail must be provided");
        }
        assertProfileOwned(profileId, userEmail);

        HexScoringConfig cfg = rawScoringSupport.buildConfigForProfile(profileId);
        double baseWeight =
                switch (type) {
                    case DRIVER ->
                            rawScoringSupport
                                    .findDriverWeight(cfg, tag)
                                    .orElseThrow(
                                            () ->
                                                    new IllegalArgumentException(
                                                            "tag not in profile for DRIVER: " + tag));
                    case COMPETITOR ->
                            rawScoringSupport
                                    .findCompetitorWeight(cfg, tag)
                                    .orElseThrow(
                                            () ->
                                                    new IllegalArgumentException(
                                                            "tag not in profile for COMPETITOR: " + tag));
                };

        long center = h3.latLngToCell(lat, lng, h3Resolution);
        List<Long> disk = h3.gridDisk(center, K_RING);
        List<String> h3Indices = new ArrayList<>(disk.size());
        for (Long cell : disk) {
            h3Indices.add(h3.h3ToString(cell));
        }

        List<Double> adjustedRaw = new ArrayList<>(h3Indices.size());
        for (int i = 0; i < h3Indices.size(); i++) {
            String h3Index = h3Indices.get(i);
            long cell = disk.get(i);
            int ring = (int) h3.gridDistance(center, cell);
            double attenuation = ringAttenuation(ring);
            double delta = attenuation * baseWeight * (type == SimulationImpactType.DRIVER ? 1.0 : -1.0);
            double raw = rawScoringSupport.computeRaw(h3Index, cfg) + delta;
            adjustedRaw.add(raw);
        }

        double[] minMax = HexagonScoringService.minMaxOrNeutral(adjustedRaw);
        double min = minMax[0];
        double max = minMax[1];
        boolean flat = minMax[2] > 0.5;

        List<HexagonMapResponse> affected = new ArrayList<>();
        Map<String, Double> redisWrites = new LinkedHashMap<>();
        for (int i = 0; i < h3Indices.size(); i++) {
            String h3Index = h3Indices.get(i);
            double r = adjustedRaw.get(i);
            double normalized = flat ? 50.0 : 100.0 * (r - min) / (max - min);
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
        String pattern = "sim_score:" + sessionId.trim() + ":*";
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
            log.warn("Redis SCAN/delete failed for simulation session {}: {}", sessionId, e.getMessage());
        }
        log.debug("Simulation session {} cleared: {} key(s)", sessionId, deleted);
    }

    private void assertProfileOwned(UUID profileId, String userEmail) {
        User user = userService.getUserByEmail(userEmail);
        DynamicProfile profile =
                dynamicProfileRepository
                        .findById(profileId)
                        .orElseThrow(() -> new NoSuchElementException("Profile not found"));
        if (profile.getUserId() == null || !profile.getUserId().equals(user.getId())) {
            throw new AccessDeniedException("You do not own this profile");
        }
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
}
