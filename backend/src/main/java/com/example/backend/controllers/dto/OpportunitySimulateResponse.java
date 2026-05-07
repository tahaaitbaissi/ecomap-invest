package com.example.backend.controllers.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;

@Schema(description = "Opportunity score and factor breakdown")
public record OpportunitySimulateResponse(
        @Schema(description = "0–100 opening opportunity for this profile at the point") double opportunityScore,
        @Schema(description = "Estimated local demand anchors (0–100)") double demandScore,
        @Schema(description = "Penalty from nearby same-type competition (0–40)") double competitionPenalty,
        @Schema(description = "Cluster / validated-market adjustment (can be negative penalty reduction)") double clusterEffectBonus,
        @Schema(description = "Fit vs preferred surroundings from business_fit_rules.json") double businessFitBonus,
        @Schema(description = "H3 cell covering the point") String h3Index,
        @Schema(description = "Unweighted sum of competitor-tag POIs within competitor radius (~500 m)")
                long competitorCountNearby,
        @Schema(description = "Unweighted sum of competitor-tag POIs inside the same H3 cell as the heatmap")
                long competitorCountInHex,
        @JsonInclude(JsonInclude.Include.ALWAYS) @Schema(description = "Matched archetype id from rules")
                String archetypeId,
        @JsonInclude(JsonInclude.Include.ALWAYS) @Schema(description = "Optional NL explanation") String explanation,
        @JsonInclude(JsonInclude.Include.ALWAYS)
                @Schema(description = "Extra numeric detail for dashboards / LLM grounding")
                Map<String, Double> metrics) {}
