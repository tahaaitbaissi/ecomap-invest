package com.example.backend.controllers.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Schema(description = "Aggregated stats for one H3 cell and profile")
public record ZoneStatsResponse(
        String h3Index,
        UUID profileId,
        Double populationDensity,
        Integer estimatedFootTraffic,
        Map<String, Integer> driverCounts,
        Map<String, Integer> competitorCounts,
        List<TopPoiDto> topPois
) {}

