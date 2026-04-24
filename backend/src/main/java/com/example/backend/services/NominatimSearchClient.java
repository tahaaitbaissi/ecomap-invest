package com.example.backend.services;

import com.example.backend.services.GeocodingService.GeocodingResult;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
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

    public NominatimSearchClient(@Qualifier("geocodingRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    @SuppressWarnings("unchecked")
    @CircuitBreaker(name = "nominatim", fallbackMethod = "fetchFallback")
    @Retry(name = "nominatim")
    public List<GeocodingResult> fetch(String query) {
        List<Map<String, Object>> raw = restClient.get()
                .uri(uri -> uri
                        .queryParam("q", query)
                        .queryParam("format", "json")
                        .queryParam("limit", 5)
                        .build())
                .retrieve()
                .body(List.class);

        if (raw == null) {
            return List.of();
        }

        return raw.stream()
                .map(m -> new GeocodingResult(
                        (String) m.get("display_name"),
                        Double.parseDouble(String.valueOf(m.get("lat"))),
                        Double.parseDouble(String.valueOf(m.get("lon")))))
                .toList();
    }

    @SuppressWarnings("unused")
    List<GeocodingResult> fetchFallback(String query, Throwable t) {
        log.warn("Nominatim circuit open or error for query: {}: {}", query, t != null ? t.getMessage() : "null");
        return List.of();
    }
}
