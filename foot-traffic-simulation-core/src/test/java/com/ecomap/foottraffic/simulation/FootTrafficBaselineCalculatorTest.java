package com.ecomap.foottraffic.simulation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class FootTrafficBaselineCalculatorTest {

    private static FootTrafficZoneParamsSnapshot params() {
        Double[] curve = new Double[24];
        for (int i = 0; i < 24; i++) curve[i] = 1d;
        curve[12] = 2d;
        return new FootTrafficZoneParamsSnapshot(
                "TEST",
                100,
                500,
                20.0,
                10_000.0,
                1.0,
                curve,
                0.10);
    }

    @Test
    void deterministic_for_same_inputs() {
        var counts = new PoiTagCounts(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 10);
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
                new PoiTagCounts(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1),
                p,
                0,
                0,
                "a",
                1L);

        var high = FootTrafficBaselineCalculator.calc(
                new PoiTagCounts(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 20),
                p,
                10_000,
                150_000,
                "b",
                1L);

        assertThat(high.baselineDaily()).isGreaterThan(low.baselineDaily());
        assertThat(high.baselineDaily()).isBetween(p.baseDailyMin(), p.baseDailyMax() * 2);
    }

    @Test
    void peak_hourly_derived_from_max_curve_share() {
        var p = params();
        var counts = new PoiTagCounts(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 10);

        var r = FootTrafficBaselineCalculator.calc(counts, p, 5_000, 0, "z", 0L);

        double expected = r.baselineDaily() * (2.0 / 24.0);
        assertThat((double) r.peakHourly()).isCloseTo(expected, org.assertj.core.data.Offset.offset(1.0));
    }
}
