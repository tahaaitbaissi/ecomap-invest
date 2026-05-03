package com.example.backend.services;

import com.example.backend.services.GeocodingService.GeocodingResult;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Outbound Nominatim API ({@link RestClient} — Spring 6+ equivalent to RestTemplate for HTTP).
 * {@link GeocodingService} adds Caffeine caching on top.
 */
@Slf4j
@Component
public class NominatimSearchClient {

    private final RestClient restClient;

    public NominatimSearchClient(@Qualifier("geocodingRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    /**
     * Single best match for Morocco (spec: limit=1, countrycodes=MA).
     */
    @SuppressWarnings("unchecked")
    @CircuitBreaker(name = "nominatim", fallbackMethod = "fetchFirstFallback")
    @Retry(name = "nominatim")
    public Optional<GeocodingResult> fetchFirst(String query) {
        List<Map<String, Object>> raw = restClient.get()
                .uri(uri -> uri
                        .queryParam("q", query)
                        .queryParam("format", "json")
                        .queryParam("limit", 1)
                        .queryParam("countrycodes", "MA")
                        .build())
                .retrieve()
                .body(List.class);

        if (raw == null || raw.isEmpty()) {
            return Optional.empty();
        }
        Map<String, Object> m = raw.get(0);
        return Optional.of(new GeocodingResult(
                (String) m.get("display_name"),
                Double.parseDouble(String.valueOf(m.get("lat"))),
                Double.parseDouble(String.valueOf(m.get("lon")))));
    }

    @SuppressWarnings("unused")
    Optional<GeocodingResult> fetchFirstFallback(String query, Throwable t) {
        log.warn("Nominatim circuit open or error for query: {}: {}", query, t != null ? t.getMessage() : "null");
        return Optional.empty();
    }
}
