package com.example.backend.scoring;

/**
 * Saturation score aligned with the RMI node {@code ScoringRemoteImpl} in module {@code rmi-scoring-server};
 * keep formulas in sync when editing.
 */
public final class SaturationFormula {

    private SaturationFormula() {}

    /**
     * @param densityNormalized population / traffic proxy in [0, 1] (caller may clamp)
     * @return score in [0, 100]
     */
    public static double apply(int drivers, int competitors, double densityNormalized) {
        double p = Math.max(0.0, Math.min(1.0, densityNormalized));
        double raw = (drivers * 0.5d) + (p * 0.3d * 100.0d) - (competitors * 1.2d);
        return Math.max(0.0, Math.min(100.0, raw));
    }
}
