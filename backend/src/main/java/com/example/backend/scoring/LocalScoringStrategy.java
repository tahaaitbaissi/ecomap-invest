package com.example.backend.scoring;

import java.util.concurrent.CompletableFuture;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
@Qualifier("local")
public class LocalScoringStrategy implements ScoringStrategy {

    @Override
    public CompletableFuture<Double> computeSaturationScore(
            int drivers, int competitors, double densityNormalized) {
        return CompletableFuture.completedFuture(
                SaturationFormula.apply(drivers, competitors, densityNormalized));
    }
}
