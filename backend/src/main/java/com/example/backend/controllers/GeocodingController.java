package com.example.backend.controllers;

import com.example.backend.controllers.dto.GeocodingSuggestionResponse;
import com.example.backend.services.GeocodingService;
import com.example.backend.services.GeocodingService.GeocodingResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/geocode")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('INVESTOR','ADMIN')")
public class GeocodingController {

    private final GeocodingService geocodingService;

    @Operation(summary = "Geocode a free-text query")
    @GetMapping
    public ResponseEntity<GeocodingResult> search(@RequestParam("q") String query) {
        if (query == null || query.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        return geocodingService
                .geocode(query.trim())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Suggest multiple geocoding matches (Casablanca-biased)")
    @GetMapping("/suggest")
    public ResponseEntity<List<GeocodingSuggestionResponse>> suggest(
            @RequestParam("q") String q, @RequestParam(value = "limit", defaultValue = "8") int limit) {
        if (q == null || q.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        int capped = Math.min(Math.max(limit, 1), 10);
        return ResponseEntity.ok(geocodingService.suggest(q.trim(), capped));
    }
}
