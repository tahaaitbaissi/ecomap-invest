package com.example.backend.foottraffic.simulation;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.backend.foottraffic.entities.FootTrafficZoneParams;
import org.junit.jupiter.api.Test;

class FootTrafficBaselineCalculatorTest {

    private static FootTrafficZoneParams params() {
        var p = new FootTrafficZoneParams();
        p.setArchetype("TEST");
        p.setBaseDailyMin(100);
        p.setBaseDailyMax(500);
        p.setPoiDensityCap(20.0);
        p.setPopDensityCap(10_000.0);
        p.setIncomeWeight(1.0);
        p.setNoiseSigma(0.10);
        p.setDayScalerSat(1.0);
        p.setDayScalerSun(1.0);
        p.setSeasonalScalers(new Double[] {1d, 1d, 1d, 1d, 1d, 1d, 1d, 1d, 1d, 1d, 1d, 1d});

        // Mostly flat, one bigger peak.
        Double[] curve = new Double[24];
        for (int i = 0; i < 24; i++) curve[i] = 1d;
        curve[12] = 2d;
        p.setHourlyCurveWd(curve);
        p.setHourlyCurveSat(curve);
        p.setHourlyCurveSun(curve);
        return p;
    }

    @Test
    void deterministic_for_same_inputs() {
        var counts = new PoiTagCounts(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 10);
        var p = params();

        var r1 = FootTrafficBaselineCalculator.calc(counts, p, 2500, 50_000, "8928308280fffff", 123L);
        var r2 = FootTrafficBaselineCalculator.calc(counts, p, 2500, 50_000, "8928308280fffff", 123L);

        assertThat(r1).isEqualTo(r2);
        assertThat(r1.noiseSeed()).isEqualTo(FootTrafficMath.murmurHash64("8928308280fffff", 123L));
    }

    @Test
    void baseline_increases_with_more_pois_and_density() {
        var p = params();

        var low = FootTrafficBaselineCalculator.calc(
                new PoiTagCounts(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1),
                p,
                0,
                0,
                "a",
                1L);

        var high = FootTrafficBaselineCalculator.calc(
                new PoiTagCounts(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 20),
                p,
                10_000,
                150_000,
                "b",
                1L);

        assertThat(high.baselineDaily()).isGreaterThan(low.baselineDaily());
        assertThat(high.baselineDaily()).isBetween(p.getBaseDailyMin(), p.getBaseDailyMax() * 2);
    }

    @Test
    void peak_hourly_derived_from_max_curve_share() {
        var p = params();
        var counts = new PoiTagCounts(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 10);

        var r = FootTrafficBaselineCalculator.calc(counts, p, 5_000, 0, "z", 0L);

        // maxWd is 2 -> peakShare = 2/24
        double expected = r.baselineDaily() * (2.0 / 24.0);
        assertThat((double) r.peakHourly()).isCloseTo(expected, org.assertj.core.data.Offset.offset(1.0));
    }
}

