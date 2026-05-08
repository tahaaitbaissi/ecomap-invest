package com.example.backend.foottraffic.simulation;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.backend.foottraffic.entities.FootTrafficZoneParams;
import org.junit.jupiter.api.Test;

class FootTrafficHourlyCurveGeneratorTest {

    private static FootTrafficZoneParams paramsWithPeakAt(int peakHour) {
        var p = new FootTrafficZoneParams();
        p.setDayScalerSat(0.8);
        p.setDayScalerSun(0.6);
        p.setSeasonalScalers(new Double[] {1d, 1d, 1d, 2d, 1d, 1d, 1d, 1d, 1d, 1d, 1d, 1d}); // April=2x

        Double[] curve = new Double[24];
        for (int i = 0; i < 24; i++) curve[i] = 1d;
        curve[peakHour] = 4d;
        p.setHourlyCurveWd(curve);
        p.setHourlyCurveSat(curve);
        p.setHourlyCurveSun(curve);
        return p;
    }

    @Test
    void sum_matches_budget_after_scalers() {
        var p = paramsWithPeakAt(9);
        int baselineDaily = 200;

        // April (index 3) seasonal 2x, SAT day scaler 0.8 => budget = 200 * 2 * 0.8 = 320
        double[] hourly = FootTrafficHourlyCurveGenerator.generate(p, DayType.SAT, 3, baselineDaily);
        assertThat(hourly).hasSize(24);
        assertThat(FootTrafficHourlyCurveGenerator.sum(hourly)).isCloseTo(320.0, org.assertj.core.data.Offset.offset(1e-9));
    }

    @Test
    void peakHourIndex_finds_peak() {
        var p = paramsWithPeakAt(18);
        double[] hourly = FootTrafficHourlyCurveGenerator.generate(p, DayType.WD, 0, 100);
        assertThat(FootTrafficHourlyCurveGenerator.peakHourIndex(hourly)).isEqualTo(18);
    }

    @Test
    void fallback_to_flat_when_sumMult_non_positive() {
        var p = new FootTrafficZoneParams();
        p.setSeasonalScalers(new Double[] {1d, 1d, 1d, 1d, 1d, 1d, 1d, 1d, 1d, 1d, 1d, 1d});
        p.setDayScalerSat(1.0);
        p.setDayScalerSun(1.0);

        Double[] curve = new Double[24];
        for (int i = 0; i < 24; i++) curve[i] = 0d;
        p.setHourlyCurveWd(curve);
        p.setHourlyCurveSat(curve);
        p.setHourlyCurveSun(curve);

        double[] hourly = FootTrafficHourlyCurveGenerator.generate(p, DayType.WD, 0, 240);
        assertThat(hourly).hasSize(24);
        assertThat(hourly[0]).isEqualTo(10.0);
        assertThat(FootTrafficHourlyCurveGenerator.sum(hourly)).isEqualTo(240.0);
    }
}

