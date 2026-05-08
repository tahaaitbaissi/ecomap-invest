package com.example.backend.controllers.dto.foottraffic;

import java.util.List;
import java.util.Map;

public record FootTrafficAuditResponse(
        String h3Index,
        String archetype,
        double archetypeConfidence,
        int baselineDaily,
        int peakHourly,
        int driverPoiCount,
        int competitorPoiCount,
        int transitPoiCount,
        double popDensity,
        double avgIncome,
        long noiseSeed,
        String computedAt,
        Map<String, Long> poiTagCountsSample,
        List<Double> hourlyWeekday,
        List<Double> hourlySaturday,
        List<Double> hourlySunday,
        double seasonalScalerJune) {}
