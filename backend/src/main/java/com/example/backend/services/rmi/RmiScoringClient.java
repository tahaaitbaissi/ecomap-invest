package com.example.backend.services.rmi;

import com.example.backend.scoring.SaturationFormula;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import com.example.backend.config.RmiScoringExecutorConfiguration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * Resilience-wrapped RMI facade over {@link RmiSaturationInvoker}.
 * <ul>
 *   <li>{@link #computeSaturationScore} — REST demos ({@code /api/v1/rmi/*}); fallback {@code 50.0}.</li>
 *   <li>{@link #computeSaturationScoreForPipeline} — {@link com.example.backend.scoring.DistributedScoringStrategy};
 *       circuit breaker / retry / timeout; fallback {@link SaturationFormula} (same as RMI node).</li>
 * </ul>
 */
@Slf4j
@Service
public class RmiScoringClient {

    private final RmiSaturationInvoker rmiSaturationInvoker;
    private final Executor rmiScoringExecutor;

    public RmiScoringClient(
            RmiSaturationInvoker rmiSaturationInvoker,
            @Qualifier(RmiScoringExecutorConfiguration.RMI_SCORING_EXECUTOR) Executor rmiScoringExecutor) {
        this.rmiSaturationInvoker = rmiSaturationInvoker;
        this.rmiScoringExecutor = rmiScoringExecutor;
    }

    /**
     * Compute saturation score with resilience: circuit breaker (fail-fast), retry, timeout.
     * Falls back to neutral score (50.0) if RMI is unavailable.
     */
    @CircuitBreaker(
        name = "rmiScoring",
        fallbackMethod = "fallbackComputeSaturationScore"
    )
    @Retry(
        name = "rmiScoring",
        fallbackMethod = "fallbackComputeSaturationScore"
    )
    @TimeLimiter(
        name = "rmiScoring",
        fallbackMethod = "fallbackComputeSaturationScore"
    )
    public CompletableFuture<Double> computeSaturationScore(
            int drivers, int competitors, double densityNormalized) {
        return CompletableFuture.supplyAsync(
                () -> {
                    try {
                        return rmiSaturationInvoker.invokeComputeSaturationScore(
                                drivers, competitors, densityNormalized);
                    } catch (Exception e) {
                        throw new RuntimeException("RMI scoring failed", e);
                    }
                },
                rmiScoringExecutor);
    }

    /**
     * Application scoring path: same RMI call as REST, with separate resilience instance and formula fallback
     * (not 50.0) when the remote node is slow, down, or the circuit is open.
     */
    @CircuitBreaker(
            name = "rmiScoringPipeline",
            fallbackMethod = "fallbackComputeSaturationScoreForPipeline")
    @Retry(
            name = "rmiScoringPipeline",
            fallbackMethod = "fallbackComputeSaturationScoreForPipeline")
    @TimeLimiter(
            name = "rmiScoringPipeline",
            fallbackMethod = "fallbackComputeSaturationScoreForPipeline")
    public CompletableFuture<Double> computeSaturationScoreForPipeline(
            int drivers, int competitors, double densityNormalized) {
        return CompletableFuture.supplyAsync(
                () -> {
                    try {
                        return rmiSaturationInvoker.invokeComputeSaturationScore(
                                drivers, competitors, densityNormalized);
                    } catch (Exception e) {
                        throw new RuntimeException("RMI scoring failed", e);
                    }
                },
                rmiScoringExecutor);
    }

    private CompletableFuture<Double> fallbackComputeSaturationScoreForPipeline(
            int drivers, int competitors, double densityNormalized, Exception e) {
        log.warn(
                "RMI pipeline scoring unavailable, using local saturation formula fallback. Cause: {}",
                e.getMessage());
        rmiSaturationInvoker.resetStub();
        return CompletableFuture.completedFuture(
                SaturationFormula.apply(drivers, competitors, densityNormalized));
    }

    @CircuitBreaker(name = "rmiScoring", fallbackMethod = "fallbackPing")
    @Retry(name = "rmiScoring", fallbackMethod = "fallbackPing")
    @TimeLimiter(name = "rmiScoring", fallbackMethod = "fallbackPing")
    public CompletableFuture<String> ping() {
        return CompletableFuture.supplyAsync(
                () -> {
                    try {
                        return rmiSaturationInvoker.invokePing();
                    } catch (Exception e) {
                        throw new RuntimeException("RMI ping failed", e);
                    }
                },
                rmiScoringExecutor);
    }

    private CompletableFuture<Double> fallbackComputeSaturationScore(
            int drivers, int competitors, double densityNormalized, Exception e) {
        log.error("RMI saturation score unavailable, using fallback (neutral score 50.0). Cause: {}",
                e.getMessage());
        rmiSaturationInvoker.resetStub();
        return CompletableFuture.completedFuture(50.0);
    }

    private CompletableFuture<String> fallbackPing(Exception e) {
        log.warn("RMI ping unavailable: {}", e.getMessage());
        rmiSaturationInvoker.resetStub();
        return CompletableFuture.completedFuture("RMI Service Unavailable (fallback)");
    }

    public void resetStub() {
        rmiSaturationInvoker.resetStub();
    }
}
