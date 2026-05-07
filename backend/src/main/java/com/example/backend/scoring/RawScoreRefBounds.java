package com.example.backend.scoring;

/**
 * Endpoints ({@code min}/{@code max}) of the linear map from raw hex score to 0–100 display. These
 * are usually percentile-based robust bounds (e.g. p5/p95) rather than literal global extremes; see
 * {@link com.example.backend.services.ProfileScoreScaleService}.
 */
public record RawScoreRefBounds(double min, double max) {

    public boolean flat() {
        return !Double.isFinite(min)
                || !Double.isFinite(max)
                || min == max;
    }

    /** Degenerate baseline (whole grid same raw). */
    public static RawScoreRefBounds degenerate(double v) {
        return new RawScoreRefBounds(v, v);
    }
}
