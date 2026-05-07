package com.example.backend.controllers.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.UUID;

/**
 * Deterministic breakdown of a hex (or aggregated parent) score for sidebar UI and LLM grounding.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record HexExplanationContextDto(
        UUID profileId,
        String profileName,
        String profileUserQuery,
        String h3Index,
        /** H3 resolution of {@link #h3Index}. */
        int h3InputResolution,
        /** True when {@link #h3Index} is coarser than the grid sampling resolution (see note). */
        boolean aggregatedFromGridLeaves,
        int gridLeafResolution,
        /** Number of res-{@link #gridLeafResolution} seeded grid indices used under this cell. */
        int gridLeafCount,
        String aggregationInterpretationNote,
        double centerLat,
        double centerLng,
        double averageRawAcrossLeaves,
        /** Normalization stretch endpoints (typically percentiles); see ProfileScoreScaleService. */
        double normalizationStretchLow,
        double normalizationStretchHigh,
        boolean normalizationFlat,
        /** Display score aligned with heatmap formula when viewport count is supplied; optional. */
        Double computedDisplayScore,
        /** Population term averaged across sampled leaves when aggregated. */
        double averagePopulationTerm,
        /** Sum of weighted driver scores across disjoint leaves (= union of children's drivers). */
        double summedWeightedDriversAcrossLeaves,
        /** Sum of weighted competitor scores across leaves. */
        double summedWeightedCompetitorsAcrossLeaves,
        List<TagContributionRow> drivers,
        List<TagContributionRow> competitors,
        DemographicsSnapshot demographics,
        long totalCompetitorPoisUnweightedAcrossLeaves,
        Double populationDensityAvg,
        Double avgIncomeAvg) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record TagContributionRow(
            String tag, double weight, long countInsideAcrossLeaves, double weightedContributionAcrossLeaves) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record DemographicsSnapshot(
            Boolean usingDemographics, Double densityCapUsed, Double populationTermAverage) {}
}
