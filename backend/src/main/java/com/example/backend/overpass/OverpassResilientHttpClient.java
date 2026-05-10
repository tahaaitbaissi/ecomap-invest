package com.example.backend.overpass;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Single Overpass HTTP attempt wrapped in Resilience4j (circuit breaker + retry + time limiter).
 * {@link OverpassApiClient} rotates URLs across mirrors; an open circuit fails fast for that URL
 * attempt so the outer loop can switch mirrors.
 */
@Component
public class OverpassResilientHttpClient {

    private final RestTemplate overpassRestTemplate;
    private final Executor overpassExecutor;

    public OverpassResilientHttpClient(
            @Qualifier(com.example.backend.config.HttpIntegrationConfiguration.OVERPASS_REST_TEMPLATE)
                    RestTemplate overpassRestTemplate,
            @Qualifier("overpassExecutor") Executor overpassExecutor) {
        this.overpassRestTemplate = overpassRestTemplate;
        this.overpassExecutor = overpassExecutor;
    }

    @CircuitBreaker(name = "overpassApi", fallbackMethod = "postForBodyFallback")
    @Retry(name = "overpassApi")
    @TimeLimiter(name = "overpassApi")
    public CompletionStage<String> postForBody(String url, HttpEntity<?> request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                ResponseEntity<String> response =
                        overpassRestTemplate.postForEntity(url, request, String.class);
                if (response.getStatusCode().is2xxSuccessful()) {
                    return response.getBody();
                }
                throw new IllegalStateException("Overpass HTTP " + response.getStatusCode().value());
            } catch (RestClientException ex) {
                throw new IllegalStateException("Overpass request failed: " + ex.getMessage(), ex);
            }
        }, overpassExecutor);
    }

    @SuppressWarnings("unused")
    private CompletionStage<String> postForBodyFallback(String url, HttpEntity<?> request, Throwable t) {
        CompletableFuture<String> f = new CompletableFuture<>();
        f.completeExceptionally(
                t != null ? t : new IllegalStateException("Overpass circuit open or degraded"));
        return f;
    }
}

