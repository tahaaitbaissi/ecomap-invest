package com.example.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Axis-aligned pilot envelope (PostGIS CRS 4326). Must stay in sync with Flyway pruning and frontend maxBounds. */
@ConfigurationProperties(prefix = "app.hexagon.study-area")
public record CasablancaStudyAreaProperties(double swLng, double swLat, double neLng, double neLat) {
    public CasablancaStudyAreaProperties {
        if (!Double.isFinite(swLng)
                || !Double.isFinite(swLat)
                || !Double.isFinite(neLng)
                || !Double.isFinite(neLat)) {
            throw new IllegalArgumentException("study-area coordinates must be finite");
        }
        if (swLng >= neLng || swLat >= neLat) {
            throw new IllegalArgumentException("study-area must have swLng < neLng and swLat < neLat");
        }
    }
}
