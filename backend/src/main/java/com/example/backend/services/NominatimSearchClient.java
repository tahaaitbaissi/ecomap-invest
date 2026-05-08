package com.example.backend.services;

import com.example.backend.controllers.dto.GeocodingSuggestionResponse;
import com.example.backend.services.GeocodingService.GeocodingResult;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import java.util.ArrayList;
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
        logFallback("fetchFirst", query, t);
        return Optional.empty();
    }

    /**
     * Multiple matches biased to a viewbox (Casablanca study area). Nominatim: {@code bounded=1},
     * {@code viewbox=swLng,swLat,neLng,neLat}.
     */
    @SuppressWarnings("unchecked")
    @RateLimiter(name = "nominatim")
    @CircuitBreaker(name = "nominatim", fallbackMethod = "fetchManyFallback")
    @Retry(name = "nominatim")
    public List<GeocodingSuggestionResponse> fetchMany(String query, int limit, GeoViewbox viewbox) {
        int capped = Math.min(Math.max(limit, 1), 10);
        String viewboxParam =
                viewbox.swLng() + "," + viewbox.swLat() + "," + viewbox.neLng() + "," + viewbox.neLat();

        List<Map<String, Object>> raw = restClient.get()
                .uri(uri -> uri
                        .queryParam("q", query)
                        .queryParam("format", "json")
                        .queryParam("limit", capped)
                        .queryParam("countrycodes", "MA")
                        .queryParam("viewbox", viewboxParam)
                        .queryParam("bounded", "1")
                        .queryParam("addressdetails", "0")
                        .queryParam("dedupe", "1")
                        .build())
                .retrieve()
                .body(List.class);

        if (raw == null || raw.isEmpty()) {
            return List.of();
        }
        List<GeocodingSuggestionResponse> out = new ArrayList<>(raw.size());
        for (Map<String, Object> m : raw) {
            if (m == null) {
                continue;
            }
            Double southLat = null;
            Double northLat = null;
            Double westLng = null;
            Double eastLng = null;
            Object bb = m.get("boundingbox");
            if (bb instanceof List<?> list && list.size() >= 4) {
                try {
                    southLat = Double.parseDouble(String.valueOf(list.get(0)));
                    northLat = Double.parseDouble(String.valueOf(list.get(1)));
                    westLng = Double.parseDouble(String.valueOf(list.get(2)));
                    eastLng = Double.parseDouble(String.valueOf(list.get(3)));
                } catch (NumberFormatException ex) {
                    log.debug("Nominatim boundingbox parse failed: {}", ex.getMessage());
                }
            }
            String osmType = m.get("osm_type") instanceof String s ? s : null;
            String osmCategory = m.get("class") instanceof String s ? s : null;
            out.add(new GeocodingSuggestionResponse(
                    (String) m.get("display_name"),
                    Double.parseDouble(String.valueOf(m.get("lat"))),
                    Double.parseDouble(String.valueOf(m.get("lon"))),
                    southLat,
                    westLng,
                    northLat,
                    eastLng,
                    osmType,
                    osmCategory));
        }
        return out;
    }

    @SuppressWarnings("unused")
    List<GeocodingSuggestionResponse> fetchManyFallback(String query, int limit, GeoViewbox viewbox, Throwable t) {
        logFallback("fetchMany", query, t);
        return List.of();
    }

    private static void logFallback(String method, String query, Throwable t) {
        if (t == null) {
            log.warn("Nominatim {} fallback for query={}: (no throwable)", method, query);
            return;
        }
        log.warn(
                "Nominatim {} fallback for query={}: {} ({})",
                method,
                query,
                t.getMessage(),
                t.getClass().getName());
    }
}
