package com.example.backend.services.ai;

import com.example.backend.config.AiOrchestratorExecutorConfiguration;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * TCP connectivity probe to the orchestrator host:port (fast fail before opening an SSE stream).
 * Uses Resilience4j: {@code aiOrchestrator} instances in {@code application.yml}.
 */
@Slf4j
@Service
public class AiOrchestratorTcpProbe {

    private final String baseUrl;
    private final Executor probeExecutor;

    public AiOrchestratorTcpProbe(
            @Value("${app.ai.orchestrator.base-url:http://ai-orchestrator:8081}") String baseUrl,
            @Qualifier(AiOrchestratorExecutorConfiguration.AI_ORCHESTRATOR_PROBE_EXECUTOR) Executor probeExecutor) {
        this.baseUrl = baseUrl;
        this.probeExecutor = probeExecutor;
    }

    @CircuitBreaker(name = "aiOrchestrator", fallbackMethod = "reachableFallback")
    @Retry(name = "aiOrchestrator", fallbackMethod = "reachableFallback")
    @TimeLimiter(name = "aiOrchestrator", fallbackMethod = "reachableFallback")
    public CompletableFuture<Boolean> reachableAsync() {
        return CompletableFuture.supplyAsync(this::tcpOk, probeExecutor);
    }

    private boolean tcpOk() {
        try {
            URI u = URI.create(baseUrl);
            String host = u.getHost();
            if (host == null || host.isBlank()) {
                return false;
            }
            int port = u.getPort() > 0 ? u.getPort() : 8081;
            try (Socket s = new Socket()) {
                s.connect(new InetSocketAddress(host, port), 1500);
            }
            return true;
        } catch (IOException e) {
            log.debug("Orchestrator TCP probe failed: {}", e.getMessage());
            return false;
        }
    }

    @SuppressWarnings("unused")
    private CompletableFuture<Boolean> reachableFallback(Throwable t) {
        log.warn("Orchestrator probe circuit/retry/time-limit: {}", t != null ? t.getMessage() : "null");
        return CompletableFuture.completedFuture(false);
    }
}
