package com.example.backend.services;

import java.util.Optional;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class GeocodingService {

    private final NominatimSearchClient nominatimSearchClient;

    public GeocodingService(NominatimSearchClient nominatimSearchClient) {
        this.nominatimSearchClient = nominatimSearchClient;
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

    public record GeocodingResult(String displayName, double lat, double lng) {}
}
