package com.example.backend.controllers.dto.foottraffic;

import java.util.List;

public record AdminFootTrafficParamsResponse(
        String archetype,
        int baseDailyMin,
        int baseDailyMax,
        double poiDensityCap,
        double popDensityCap,
        double incomeWeight,
        List<Double> hourlyCurveWd,
        List<Double> hourlyCurveSat,
        List<Double> hourlyCurveSun,
        List<Double> seasonalScalers,
        double dayScalerSat,
        double dayScalerSun,
        double noiseSigma,
        String updatedAt) {}
