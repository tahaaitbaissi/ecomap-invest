package com.example.backend.services.rmi;

import com.example.ecomap.rmi.scoring.ScoringRemote;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Spring-side RMI client: looks up {@link ScoringRemote} from the remote registry (separate JVM).
 * Includes resilience patterns: circuit breaker, retry, timeout.
 */
@Slf4j
@Service
public class RmiScoringClient {

    @Value("${app.rmi.scoring.host:localhost}")
    private String host;

    @Value("${app.rmi.scoring.port:1099}")
    private int port;

    @Value("${app.rmi.scoring.service-name:ScoringService}")
    private String serviceName;

    private volatile ScoringRemote cached;

    /**
     * Compute saturation score with resilience: circuit breaker (fail-fast), retry (transient errors), timeout.
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
        return CompletableFuture.supplyAsync(() -> {
            try {
                ScoringRemote remote = getRemote();
                double score = remote.computeSaturationScore(drivers, competitors, densityNormalized);
                log.debug("RMI scoring completed: drivers={}, competitors={}, density={}, score={}",
                    drivers, competitors, densityNormalized, score);
                return score;
            } catch (RemoteException | NotBoundException e) {
                log.warn("RMI scoring call failed: {}", e.getMessage());
                throw new RuntimeException("RMI scoring failed", e);
            }
        });
    }

    /**
     * Health check (ping) with resilience.
     */
    @CircuitBreaker(name = "rmiScoring", fallbackMethod = "fallbackPing")
    @Retry(name = "rmiScoring", fallbackMethod = "fallbackPing")
    @TimeLimiter(name = "rmiScoring", fallbackMethod = "fallbackPing")
    public CompletableFuture<String> ping() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String result = getRemote().ping();
                log.debug("RMI ping successful: {}", result);
                return result;
            } catch (RemoteException | NotBoundException e) {
                log.warn("RMI ping failed: {}", e.getMessage());
                throw new RuntimeException("RMI ping failed", e);
            }
        });
    }

    /**
     * Fallback when RMI saturation score computation fails.
     * Returns neutral score (50.0) to allow app to continue gracefully.
     */
    private CompletableFuture<Double> fallbackComputeSaturationScore(
            int drivers, int competitors, double densityNormalized, Exception e) {
        log.error("RMI saturation score unavailable, using fallback (neutral score 50.0). Cause: {}",
            e.getMessage());
        resetStub();
        return CompletableFuture.completedFuture(50.0);
    }

    /**
     * Fallback for ping when RMI is down.
     */
    private CompletableFuture<String> fallbackPing(Exception e) {
        log.warn("RMI ping unavailable: {}", e.getMessage());
        resetStub();
        return CompletableFuture.completedFuture("RMI Service Unavailable (fallback)");
    }

    private ScoringRemote getRemote() throws RemoteException, NotBoundException {
        ScoringRemote local = cached;
        if (local != null) {
            return local;
        }
        synchronized (this) {
            if (cached == null) {
                Registry registry = LocateRegistry.getRegistry(host, port);
                cached = (ScoringRemote) registry.lookup(serviceName);
                log.info("RMI stub resolved: {} @ {}:{}", serviceName, host, port);
            }
            return cached;
        }
    }

    /**
     * Call after connection errors to force re-lookup on next request.
     * Useful for circuit breaker reset or manual recovery.
     */
    public void resetStub() {
        synchronized (this) {
            cached = null;
        }
        log.info("RMI stub reset, will re-lookup on next request");
    }
}
