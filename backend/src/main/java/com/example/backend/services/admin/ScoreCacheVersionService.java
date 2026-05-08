package com.example.backend.services.admin;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ScoreCacheVersionService {

    static final String POI_VERSION_KEY = "score:poiVersion";
    static final String DEMO_VERSION_KEY = "score:demoVersion";
    static final String TRAFFIC_VERSION_KEY = "score:trafficVersion";

    @Nullable
    private final StringRedisTemplate stringRedisTemplate;

    public long getPoiVersion() {
        return getOrInit(POI_VERSION_KEY);
    }

    public long getDemoVersion() {
        return getOrInit(DEMO_VERSION_KEY);
    }

    public long bumpPoiVersion() {
        return incr(POI_VERSION_KEY);
    }

    public long bumpDemoVersion() {
        return incr(DEMO_VERSION_KEY);
    }

    public long getTrafficVersion() {
        return getOrInit(TRAFFIC_VERSION_KEY);
    }

    public long bumpTrafficVersion() {
        return incr(TRAFFIC_VERSION_KEY);
    }

    private long getOrInit(String key) {
        if (stringRedisTemplate == null) {
            return 0L;
        }
        try {
            String v = stringRedisTemplate.opsForValue().get(key);
            if (v != null && !v.isBlank()) {
                return Long.parseLong(v);
            }
            stringRedisTemplate.opsForValue().set(key, "1", Duration.ofDays(3650));
            return 1L;
        } catch (Exception e) {
            return 0L;
        }
    }

    private long incr(String key) {
        if (stringRedisTemplate == null) {
            return 0L;
        }
        try {
            Long v = stringRedisTemplate.opsForValue().increment(key);
            if (v == null) {
                return 0L;
            }
            stringRedisTemplate.expire(key, Duration.ofDays(3650));
            return v;
        } catch (Exception e) {
            return 0L;
        }
    }
}

