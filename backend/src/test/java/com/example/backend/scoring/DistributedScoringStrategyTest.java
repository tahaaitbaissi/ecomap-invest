package com.example.backend.scoring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.example.backend.services.rmi.RmiScoringClient;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DistributedScoringStrategyTest {

    @Mock
    private RmiScoringClient rmiScoringClient;

    @Test
    void delegates_pipeline_fallback_result_from_client() {
        double formula = SaturationFormula.apply(2, 1, 0.8);
        when(rmiScoringClient.computeSaturationScoreForPipeline(2, 1, 0.8))
                .thenReturn(CompletableFuture.completedFuture(formula));

        DistributedScoringStrategy s = new DistributedScoringStrategy(rmiScoringClient);
        assertEquals(23.8, s.computeSaturationScore(2, 1, 0.8).join(), 1e-9);
    }

    @Test
    void uses_remote_value_when_client_returns_ok() {
        when(rmiScoringClient.computeSaturationScoreForPipeline(0, 0, 0.5))
                .thenReturn(CompletableFuture.completedFuture(42.0));

        DistributedScoringStrategy s = new DistributedScoringStrategy(rmiScoringClient);
        assertEquals(42.0, s.computeSaturationScore(0, 0, 0.5).join(), 1e-9);
    }
}
