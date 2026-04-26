package com.example.backend.controllers.dto;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ZoneStatsResponse(
        String h3Index,
        UUID profileId,
        Double populationDensity,
        Integer estimatedFootTraffic,
        Map<String, Integer> driverCounts,
        Map<String, Integer> competitorCounts,
        List<TopPoiDto> topPois
) {}

