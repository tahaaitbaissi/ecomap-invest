package com.example.backend.controllers.dto;

import java.util.List;
import java.util.UUID;

/**
 * Debug payload for viewport-independent heatmap normalization: raw-score distribution over the same
 * reference H3 grid sample used by {@link com.example.backend.services.ProfileScoreScaleService}.
 */
public record ProfileScoreNormDebugResponse(
        UUID profileId,
        int sampleSize,
        int maxReferenceCells,
        double globalMinRaw,
        double globalMaxRaw,
        double percentileLowRaw,
        double percentile50Raw,
        double percentileHighRaw,
        /** Low endpoint of the linear stretch to 0..100 ({@link #stretchMode}). */
        double stretchLowUsed,
        /** High endpoint of the linear stretch to 0..100 ({@link #stretchMode}). */
        double stretchHighUsed,
        StretchMode stretchMode,
        /** Equal-width histogram over {@link #globalMinRaw}…{@link #globalMaxRaw} for quick shape checks. */
        List<HistogramBucket> histogram) {

    public enum StretchMode {
        PERCENTILE,
        MIN_MAX,
        DEGENERATE
    }

    public record HistogramBucket(double lowInclusive, double highInclusive, int count) {}
}
