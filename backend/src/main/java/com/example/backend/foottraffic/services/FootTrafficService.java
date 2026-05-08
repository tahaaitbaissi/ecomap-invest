package com.example.backend.foottraffic.services;

import com.example.backend.foottraffic.config.FootTrafficProperties;
import com.example.backend.foottraffic.entities.FootTrafficCellProfile;
import com.example.backend.foottraffic.entities.FootTrafficZoneParams;
import com.example.backend.foottraffic.repositories.FootTrafficCellProfileRepository;
import com.example.backend.foottraffic.repositories.FootTrafficZoneParamsRepository;
import com.example.backend.foottraffic.simulation.DayType;
import com.example.backend.foottraffic.simulation.FootTrafficHourlyCurveGenerator;
import com.example.backend.services.admin.ScoreCacheVersionService;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class FootTrafficService {

    private final FootTrafficProperties properties;
    private final FootTrafficCellProfileRepository cellProfileRepository;
    private final FootTrafficZoneParamsRepository zoneParamsRepository;
    private final ScoreCacheVersionService scoreCacheVersionService;

    @Nullable
    private final StringRedisTemplate stringRedisTemplate;

    public OptionalDouble getPeakHourly(String h3Index) {
        if (!properties.isEnabled() || h3Index == null || h3Index.isBlank()) {
            return OptionalDouble.empty();
        }
        long tv = scoreCacheVersionService.getTrafficVersion();
        String redisKey = peakRedisKey(tv, h3Index);
        if (stringRedisTemplate != null) {
            try {
                String v = stringRedisTemplate.opsForValue().get(redisKey);
                if (v != null && !v.isBlank()) {
                    return OptionalDouble.of(Long.parseLong(v.trim()));
                }
            } catch (Exception e) {
                log.debug("Redis GET foot-traffic peak failed for {}: {}", h3Index, e.getMessage());
            }
        }
        return cellProfileRepository
                .findById(h3Index)
                .map(p -> OptionalDouble.of(p.getPeakHourly()))
                .orElse(OptionalDouble.empty());
    }

    public OptionalDouble getPeakHourlyNorm(String h3Index) {
        OptionalDouble peak = getPeakHourly(h3Index);
        if (peak.isEmpty()) {
            return OptionalDouble.empty();
        }
        double cap = Math.max(1.0, properties.getTrafficCap());
        return OptionalDouble.of(
                Math.min(1.0, Math.max(0.0, peak.getAsDouble() / cap)));
    }

    /** Hourly pedestrians for UI/explain; computed from stored profile + zone params (not Redis). */
    public Optional<double[]> getHourly(String h3Index, DayType dayType, int monthIndex) {
        if (!properties.isEnabled() || h3Index == null || h3Index.isBlank()) {
            return Optional.empty();
        }
        Optional<FootTrafficCellProfile> prof = cellProfileRepository.findById(h3Index);
        if (prof.isEmpty()) {
            return Optional.empty();
        }
        Optional<FootTrafficZoneParams> params = zoneParamsRepository.findById(prof.get().getArchetype());
        if (params.isEmpty()) {
            return Optional.empty();
        }
        double[] hourly =
                FootTrafficHourlyCurveGenerator.generate(
                        params.get(), dayType, monthIndex, prof.get().getBaselineDaily());
        return Optional.of(hourly);
    }

    public Optional<FootTrafficCellProfile> getProfile(String h3Index) {
        if (h3Index == null || h3Index.isBlank()) {
            return Optional.empty();
        }
        return cellProfileRepository.findById(h3Index);
    }

    public Optional<FootTrafficZoneParams> getZoneParams(String archetype) {
        if (archetype == null || archetype.isBlank()) {
            return Optional.empty();
        }
        return zoneParamsRepository.findById(archetype);
    }

    /**
     * Batch peak lookup: Redis pipeline first, then DB for misses.
     */
    public List<OptionalDouble> getPeakHourlyBatch(List<String> h3Indices) {
        List<OptionalDouble> out = new ArrayList<>(h3Indices.size());
        if (!properties.isEnabled() || h3Indices.isEmpty()) {
            for (int i = 0; i < h3Indices.size(); i++) {
                out.add(OptionalDouble.empty());
            }
            return out;
        }
        long tv = scoreCacheVersionService.getTrafficVersion();
        List<String> keys = h3Indices.stream().map(h -> peakRedisKey(tv, h)).toList();

        List<String> redisVals = null;
        if (stringRedisTemplate != null) {
            try {
                redisVals = stringRedisTemplate.opsForValue().multiGet(keys);
            } catch (DataAccessException e) {
                log.debug("Redis MGET foot-traffic failed: {}", e.getMessage());
            }
        }

        for (int i = 0; i < h3Indices.size(); i++) {
            String h = h3Indices.get(i);
            String rv = redisVals != null && i < redisVals.size() ? redisVals.get(i) : null;
            if (rv != null && !rv.isBlank()) {
                try {
                    out.add(OptionalDouble.of(Long.parseLong(rv.trim())));
                    continue;
                } catch (NumberFormatException ignored) {
                    // fall through to DB
                }
            }
            out.add(
                    cellProfileRepository
                            .findById(h)
                            .map(p -> OptionalDouble.of(p.getPeakHourly()))
                            .orElse(OptionalDouble.empty()));
        }
        return out;
    }

    /** Write peak to Redis after recompute (internal). */
    public void writePeakCache(String h3Index, long peakHourly) {
        if (!properties.isEnabled() || stringRedisTemplate == null) {
            return;
        }
        long tv = scoreCacheVersionService.getTrafficVersion();
        String key = peakRedisKey(tv, h3Index);
        int ttl = properties.getCache().getTtlSeconds();
        if (ttl <= 0) {
            return;
        }
        try {
            stringRedisTemplate.opsForValue().set(key, String.valueOf(peakHourly), Duration.ofSeconds(ttl));
        } catch (DataAccessException e) {
            log.debug("Redis SET foot-traffic peak failed for {}: {}", h3Index, e.getMessage());
        }
    }

    public void writePeakCachePipelined(long trafficVersion, List<String> h3Indices, List<Long> peaks) {
        if (!properties.isEnabled() || stringRedisTemplate == null || h3Indices.isEmpty()) {
            return;
        }
        int ttl = properties.getCache().getTtlSeconds();
        if (ttl <= 0) {
            return;
        }
        try {
            RedisSerializer<String> ser = stringRedisTemplate.getStringSerializer();
            stringRedisTemplate.executePipelined(
                    (RedisCallback<Object>)
                            connection -> {
                                for (int i = 0; i < h3Indices.size() && i < peaks.size(); i++) {
                                    byte[] kb = ser.serialize(peakRedisKey(trafficVersion, h3Indices.get(i)));
                                    byte[] vb = ser.serialize(String.valueOf(peaks.get(i)));
                                    if (kb != null && vb != null) {
                                        connection.setEx(kb, ttl, vb);
                                    }
                                }
                                return null;
                            });
        } catch (DataAccessException e) {
            log.warn("Redis pipeline foot-traffic peak failed: {}", e.getMessage());
        }
    }

    public static String peakRedisKey(long trafficVersion, String h3Index) {
        return "ftc:t" + trafficVersion + ":" + h3Index + ":peak";
    }
}
