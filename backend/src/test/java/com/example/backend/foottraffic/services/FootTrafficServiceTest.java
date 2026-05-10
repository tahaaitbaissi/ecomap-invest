package com.example.backend.foottraffic.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.backend.foottraffic.config.FootTrafficProperties;
import com.example.backend.foottraffic.entities.FootTrafficCellProfile;
import com.example.backend.foottraffic.entities.FootTrafficZoneParams;
import com.example.backend.foottraffic.repositories.FootTrafficCellProfileRepository;
import com.example.backend.foottraffic.repositories.FootTrafficZoneParamsRepository;
import com.example.backend.foottraffic.simulation.DayType;
import com.example.backend.services.admin.ScoreCacheVersionService;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class FootTrafficServiceTest {

    private static FootTrafficProperties props() {
        var p = new FootTrafficProperties();
        p.setEnabled(true);
        p.setTrafficCap(200);
        p.getCache().setTtlSeconds(60);
        return p;
    }

    @Test
    void getPeakHourly_redis_hit_skips_db() {
        var properties = props();
        var cellRepo = Mockito.mock(FootTrafficCellProfileRepository.class);
        var zoneRepo = Mockito.mock(FootTrafficZoneParamsRepository.class);
        var versions = Mockito.mock(ScoreCacheVersionService.class);
        when(versions.getTrafficVersion()).thenReturn(7L);

        StringRedisTemplate redis = Mockito.mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> vops = Mockito.mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(vops);
        when(vops.get("ftc:t7:h:peak")).thenReturn("123");

        var svc = new FootTrafficService(properties, cellRepo, zoneRepo, versions, redis);

        assertThat(svc.getPeakHourly("h")).isEqualTo(OptionalDouble.of(123));
        verify(cellRepo, never()).findById(any());
    }

    @Test
    void getPeakHourly_db_fallback_when_redis_miss() {
        var properties = props();
        var cellRepo = Mockito.mock(FootTrafficCellProfileRepository.class);
        var zoneRepo = Mockito.mock(FootTrafficZoneParamsRepository.class);
        var versions = Mockito.mock(ScoreCacheVersionService.class);
        when(versions.getTrafficVersion()).thenReturn(0L);

        StringRedisTemplate redis = Mockito.mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> vops = Mockito.mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(vops);
        when(vops.get("ftc:t0:h:peak")).thenReturn(null);

        var prof = new FootTrafficCellProfile();
        prof.setH3Index("h");
        prof.setPeakHourly(77);
        when(cellRepo.findById("h")).thenReturn(Optional.of(prof));

        var svc = new FootTrafficService(properties, cellRepo, zoneRepo, versions, redis);
        assertThat(svc.getPeakHourly("h")).isEqualTo(OptionalDouble.of(77));
    }

    @Test
    void getTrafficIntensityNorm_uses_baseline_times_peak_share_before_rounding() {
        var properties = props();
        properties.setTrafficCap(200);
        var cellRepo = Mockito.mock(FootTrafficCellProfileRepository.class);
        var zoneRepo = Mockito.mock(FootTrafficZoneParamsRepository.class);
        var versions = Mockito.mock(ScoreCacheVersionService.class);

        var prof = new FootTrafficCellProfile();
        prof.setH3Index("h");
        prof.setArchetype("A");
        prof.setBaselineDaily(120);
        prof.setPeakHourly(1);
        when(cellRepo.findById("h")).thenReturn(Optional.of(prof));

        var params = new FootTrafficZoneParams();
        params.setArchetype("A");
        Double[] curve = new Double[24];
        for (int i = 0; i < 24; i++) curve[i] = 1d;
        curve[10] = 3d;
        params.setHourlyCurveWd(curve);
        when(zoneRepo.findById("A")).thenReturn(Optional.of(params));

        var svc = new FootTrafficService(properties, cellRepo, zoneRepo, versions, null);
        double expectedPeak = 120.0 * (3.0 / 24.0);
        double ratio = expectedPeak / 200.0;
        assertThat(svc.getTrafficIntensityNorm("h").getAsDouble()).isEqualTo(Math.sqrt(ratio));
    }

    @Test
    void getPeakHourlyNorm_applies_cap_and_clamp() {
        var properties = props();
        var cellRepo = Mockito.mock(FootTrafficCellProfileRepository.class);
        var zoneRepo = Mockito.mock(FootTrafficZoneParamsRepository.class);
        var versions = Mockito.mock(ScoreCacheVersionService.class);
        when(versions.getTrafficVersion()).thenReturn(0L);

        StringRedisTemplate redis = Mockito.mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> vops = Mockito.mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(vops);
        when(vops.get("ftc:t0:h:peak")).thenReturn("500");

        var svc = new FootTrafficService(properties, cellRepo, zoneRepo, versions, redis);
        assertThat(svc.getPeakHourlyNorm("h").getAsDouble()).isEqualTo(1.0);
    }

    @Test
    void getHourly_computes_from_profile_and_params() {
        var properties = props();
        var cellRepo = Mockito.mock(FootTrafficCellProfileRepository.class);
        var zoneRepo = Mockito.mock(FootTrafficZoneParamsRepository.class);
        var versions = Mockito.mock(ScoreCacheVersionService.class);

        var prof = new FootTrafficCellProfile();
        prof.setH3Index("h");
        prof.setArchetype("A");
        prof.setBaselineDaily(240);
        when(cellRepo.findById("h")).thenReturn(Optional.of(prof));

        var params = new FootTrafficZoneParams();
        params.setArchetype("A");
        params.setDayScalerSat(1.0);
        params.setDayScalerSun(1.0);
        params.setSeasonalScalers(new Double[] {1d, 1d, 1d, 1d, 1d, 1d, 1d, 1d, 1d, 1d, 1d, 1d});
        Double[] curve = new Double[24];
        for (int i = 0; i < 24; i++) curve[i] = 1d;
        params.setHourlyCurveWd(curve);
        params.setHourlyCurveSat(curve);
        params.setHourlyCurveSun(curve);
        when(zoneRepo.findById("A")).thenReturn(Optional.of(params));

        var svc = new FootTrafficService(properties, cellRepo, zoneRepo, versions, null);
        var hourly = svc.getHourly("h", DayType.WD, 0);
        assertThat(hourly).isPresent();
        assertThat(hourly.get()).hasSize(24);
        assertThat(hourly.get()[0]).isEqualTo(10.0);
    }

    @Test
    void getPeakHourlyBatch_uses_redis_multiGet_then_db_for_miss() {
        var properties = props();
        var cellRepo = Mockito.mock(FootTrafficCellProfileRepository.class);
        var zoneRepo = Mockito.mock(FootTrafficZoneParamsRepository.class);
        var versions = Mockito.mock(ScoreCacheVersionService.class);
        when(versions.getTrafficVersion()).thenReturn(2L);

        StringRedisTemplate redis = Mockito.mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> vops = Mockito.mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(vops);

        when(vops.multiGet(List.of("ftc:t2:a:peak", "ftc:t2:b:peak")))
                .thenReturn(Arrays.asList("10", null));

        var profB = new FootTrafficCellProfile();
        profB.setH3Index("b");
        profB.setPeakHourly(20);
        when(cellRepo.findById("b")).thenReturn(Optional.of(profB));

        var svc = new FootTrafficService(properties, cellRepo, zoneRepo, versions, redis);
        var out = svc.getPeakHourlyBatch(List.of("a", "b"));

        assertThat(out).hasSize(2);
        assertThat(out.get(0)).isEqualTo(OptionalDouble.of(10));
        assertThat(out.get(1)).isEqualTo(OptionalDouble.of(20));
    }

    @Test
    void writePeakCache_sets_ttl_and_versioned_key() {
        var properties = props();
        var cellRepo = Mockito.mock(FootTrafficCellProfileRepository.class);
        var zoneRepo = Mockito.mock(FootTrafficZoneParamsRepository.class);
        var versions = Mockito.mock(ScoreCacheVersionService.class);
        when(versions.getTrafficVersion()).thenReturn(9L);

        StringRedisTemplate redis = Mockito.mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> vops = Mockito.mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(vops);

        var svc = new FootTrafficService(properties, cellRepo, zoneRepo, versions, redis);
        svc.writePeakCache("h", 111);

        ArgumentCaptor<Duration> ttl = ArgumentCaptor.forClass(Duration.class);
        verify(vops).set(eq("ftc:t9:h:peak"), eq("111"), ttl.capture());
        assertThat(ttl.getValue()).isEqualTo(Duration.ofSeconds(60));
    }
}

