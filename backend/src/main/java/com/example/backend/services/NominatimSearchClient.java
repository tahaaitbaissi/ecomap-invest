package com.example.backend.services;

import com.example.backend.services.GeocodingService.GeocodingResult;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Outbound Nominatim API calls, wrapped with Resilience4j. {@link GeocodingService} adds
 * in-memory Caffeine caching on top of this.
 */
@Slf4j
@Component
public class NominatimSearchClient {

    private final RestClient restClient;
    private final Executor nominatimExecutor;

    public NominatimSearchClient(
            @Qualifier("geocodingRestClient") RestClient restClient,
            @Qualifier("nominatimExecutor") Executor nominatimExecutor) {
        this.restClient = restClient;
        this.nominatimExecutor = nominatimExecutor;
    }

    @SuppressWarnings("unchecked")
    @CircuitBreaker(name = "nominatim", fallbackMethod = "fetchFallback")
    @Retry(name = "nominatim")
    @TimeLimiter(name = "nominatim")
    public CompletionStage<List<GeocodingResult>> fetchAsync(String query) {
        return CompletableFuture.supplyAsync(() -> {
            List<Map<String, Object>> raw = restClient.get()
                    .uri(uri -> uri
                            .queryParam("q", query)
                            .queryParam("format", "json")
                            .queryParam("limit", 5)
                            .build())
                    .retrieve()
                    .body(List.class);

            if (raw == null) {
                return List.<GeocodingResult>of();
            }

            return raw.stream()
                    .map(m -> new GeocodingResult(
                            (String) m.get("display_name"),
                            Double.parseDouble(String.valueOf(m.get("lat"))),
                            Double.parseDouble(String.valueOf(m.get("lon")))))
                    .toList();
        }, nominatimExecutor);
    }

    @SuppressWarnings("unused")
    CompletionStage<List<GeocodingResult>> fetchFallback(String query, Throwable t) {
        log.warn("Nominatim circuit open or error for query: {}: {}", query, t != null ? t.getMessage() : "null");
        return CompletableFuture.completedFuture(List.of());
    }
}
