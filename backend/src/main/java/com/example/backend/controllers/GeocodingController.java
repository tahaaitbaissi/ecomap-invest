package com.example.backend.controllers;

import com.example.backend.services.GeocodingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.example.backend.services.GeocodingService.GeocodingResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/geocode")
@RequiredArgsConstructor
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
}
