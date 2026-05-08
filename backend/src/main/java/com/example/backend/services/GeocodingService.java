package com.example.backend.services;

import com.example.backend.config.CasablancaBboxProvider;
import com.example.backend.controllers.dto.GeocodingSuggestionResponse;
import java.util.List;
import java.util.Optional;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class GeocodingService {

    private final NominatimSearchClient nominatimSearchClient;
    private final CasablancaBboxProvider casablancaBboxProvider;

    public GeocodingService(
            NominatimSearchClient nominatimSearchClient, CasablancaBboxProvider casablancaBboxProvider) {
        this.nominatimSearchClient = nominatimSearchClient;
        this.casablancaBboxProvider = casablancaBboxProvider;
    }

    /**
     * Geocode a query: first Nominatim result in Morocco, or empty. Cached; misses (empty) are
     * not stored so repeated unknown queries still hit the API unless the circuit is open.
     */
    @Cacheable(
            cacheNames = "geocode",
            condition = "#query != null && !#query.isBlank()",
            key = "#query.trim().toLowerCase()",
            unless = "#result == null || !#result.isPresent()")
    public Optional<GeocodingResult> geocode(String query) {
        if (query == null || query.isBlank()) {
            return Optional.empty();
        }
        return nominatimSearchClient.fetchFirst(query.trim());
    }

    /**
     * Multi-result suggestions biased to the Casablanca study-area viewbox. Cached per normalized
     * query and limit.
     */
    @Cacheable(
            cacheNames = "geocodeSuggest",
            condition = "#q != null && !#q.isBlank() && #limit > 0",
            key = "#q.trim().toLowerCase() + ':' + #limit")
    public List<GeocodingSuggestionResponse> suggest(String q, int limit) {
        if (q == null || q.isBlank() || limit <= 0) {
            return List.of();
        }
        return nominatimSearchClient.fetchMany(q.trim(), limit, casablancaBboxProvider.casablancaViewbox());
    }

    public record GeocodingResult(String displayName, double lat, double lng) {}
}
