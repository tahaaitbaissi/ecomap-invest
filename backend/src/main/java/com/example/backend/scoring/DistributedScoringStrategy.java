package com.example.backend.scoring;

import com.example.backend.services.rmi.RmiScoringClient;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * Distributed saturation scoring via {@link RmiScoringClient#computeSaturationScoreForPipeline}
 * (circuit breaker, retry, time limiter, formula-aligned fallback).
 */
@Component
@Qualifier("distributed")
@RequiredArgsConstructor
public class DistributedScoringStrategy implements ScoringStrategy {

    private final RmiScoringClient rmiScoringClient;

    @Override
    public CompletableFuture<Double> computeSaturationScore(
            int drivers, int competitors, double densityNormalized) {
        return rmiScoringClient.computeSaturationScoreForPipeline(drivers, competitors, densityNormalized);
    }
}
