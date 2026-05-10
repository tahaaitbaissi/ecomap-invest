package com.ecomap.foottraffic.simulation;

/** Immutable zone parameters for {@link FootTrafficBaselineCalculator} (DB-agnostic). */
public record FootTrafficZoneParamsSnapshot(
        String archetype,
        int baseDailyMin,
        int baseDailyMax,
        double poiDensityCap,
        double popDensityCap,
        double incomeWeight,
        Double[] hourlyCurveWd,
        double noiseSigma) {}
