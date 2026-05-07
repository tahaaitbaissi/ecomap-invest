package com.example.aiorchestrator.dto;

import java.util.List;

public record HexExplanationContextDto(
        String profileId,
        String profileName,
        String profileUserQuery,
        String h3Index,
        int h3InputResolution,
        boolean aggregatedFromGridLeaves,
        int gridLeafResolution,
        int gridLeafCount,
        String aggregationInterpretationNote,
        double centerLat,
        double centerLng,
        double averageRawAcrossLeaves,
        double normalizationStretchLow,
        double normalizationStretchHigh,
        boolean normalizationFlat,
        Double computedDisplayScore,
        double averagePopulationTerm,
        double summedWeightedDriversAcrossLeaves,
        double summedWeightedCompetitorsAcrossLeaves,
        List<TagContributionRow> drivers,
        List<TagContributionRow> competitors,
        DemographicsSnapshot demographics,
        long totalCompetitorPoisUnweightedAcrossLeaves,
        Double populationDensityAvg,
        Double avgIncomeAvg) {

    public record DemographicsSnapshot(Boolean usingDemographics, Double densityCapUsed, Double populationTermAverage) {}

    public record TagContributionRow(
            String tag, double weight, long countInsideAcrossLeaves, double weightedContributionAcrossLeaves) {}
}

