package com.example.backend.services.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class ScoreCacheVersionServiceTest {

    @Test
    void getTrafficVersion_initializes_to_1_when_missing() {
        StringRedisTemplate redis = Mockito.mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> vops = Mockito.mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(vops);
        when(vops.get(ScoreCacheVersionService.TRAFFIC_VERSION_KEY)).thenReturn(null);

        var svc = new ScoreCacheVersionService(redis);
        long v = svc.getTrafficVersion();

        assertThat(v).isEqualTo(1L);
        verify(vops)
                .set(
                        ScoreCacheVersionService.TRAFFIC_VERSION_KEY,
                        "1",
                        Duration.ofDays(3650));
    }

    @Test
    void bumpTrafficVersion_increments_and_extends_ttl() {
        StringRedisTemplate redis = Mockito.mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> vops = Mockito.mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(vops);
        when(vops.increment(ScoreCacheVersionService.TRAFFIC_VERSION_KEY)).thenReturn(42L);

        var svc = new ScoreCacheVersionService(redis);
        long v = svc.bumpTrafficVersion();

        assertThat(v).isEqualTo(42L);
        verify(redis).expire(ScoreCacheVersionService.TRAFFIC_VERSION_KEY, Duration.ofDays(3650));
    }
}

