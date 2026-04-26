package com.example.backend.services;

import java.util.concurrent.CompletionException;
import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class GeocodingService {

    private final NominatimSearchClient nominatimSearchClient;

    public GeocodingService(NominatimSearchClient nominatimSearchClient) {
        this.nominatimSearchClient = nominatimSearchClient;
    }

    @Cacheable(
            cacheNames = "geocode",
            condition = "#query != null && !#query.isBlank()",
            key = "#query.trim().toLowerCase()")
    public List<GeocodingResult> search(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        try {
            return nominatimSearchClient.fetchAsync(query.trim()).toCompletableFuture().join();
        } catch (CompletionException ex) {
            return List.of();
        }
    }

    public record GeocodingResult(String displayName, double lat, double lng) {}
}
