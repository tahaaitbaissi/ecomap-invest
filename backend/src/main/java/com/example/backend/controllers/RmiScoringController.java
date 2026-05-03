package com.example.backend.controllers;

import com.example.backend.controllers.dto.RmiScoreResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.example.backend.services.rmi.RmiScoringClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Demo REST facade: browser/Postman → Spring Boot → RMI → remote scoring JVM.
 * Demonstrates RMI-based distributed scoring with resilience patterns (circuit breaker, retry, timeout).
 * Public for lab demos (no JWT); tighten with {@code hasRole('ADMIN')} in production.
 */
@Slf4j
@Tag(name = "RMI scoring demo", description = "Public demo of remote JVM saturation scoring")
@RestController
@RequestMapping("/api/v1/rmi")
@RequiredArgsConstructor
public class RmiScoringController {

    private final RmiScoringClient rmiScoringClient;

    /**
     * Compute saturation score (synchronous endpoint).
     * Demonstrates resilience: RMI failures are handled gracefully with fallback (50.0).
     */
    @GetMapping("/score")
    public ResponseEntity<?> score(
            @RequestParam(defaultValue = "0") int drivers,
            @RequestParam(defaultValue = "0") int competitors,
            @RequestParam(defaultValue = "0.5") double density) {
        double densityClamped = Math.max(0.0, Math.min(1.0, density));
        try {
            // Call async RMI method and wait synchronously for the result
            double value = rmiScoringClient.computeSaturationScore(drivers, competitors, densityClamped)
                .join();  // Wait for CompletableFuture to complete
            log.debug("RMI scoring computed: drivers={}, competitors={}, density={}, score={}",
                drivers, competitors, densityClamped, value);
            return ResponseEntity.ok(new RmiScoreResponse(value, "rmi"));
        } catch (Exception e) {
            log.error("RMI scoring endpoint error: {}", e.getMessage());
            rmiScoringClient.resetStub();
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body("RMI scoring node unavailable: " + e.getMessage());
        }
    }

    /**
     * Health check (ping) endpoint.
     * Tests connectivity to RMI scoring service.
     */
    @Operation(summary = "Ping RMI scoring node")
    @GetMapping("/ping")
    public ResponseEntity<?> ping() {
        try {
            String result = rmiScoringClient.ping()
                .join();  // Wait for CompletableFuture to complete
            log.debug("RMI ping result: {}", result);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("RMI ping endpoint error: {}", e.getMessage());
            rmiScoringClient.resetStub();
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body("RMI scoring node unavailable: " + e.getMessage());
        }
    }
}
