package com.example.backend.scoring;

import java.util.concurrent.CompletableFuture;

/**
 * Pluggable saturation scoring (local JVM vs distributed RMI).
 */
public interface ScoringStrategy {

    CompletableFuture<Double> computeSaturationScore(int drivers, int competitors, double densityNormalized);
}
