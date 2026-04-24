package com.example.backend.util;

/**
 * Shared rules for public map POI / hex viewports. Bbox order: {@code swLng, swLat, neLng, neLat}.
 */
public final class ViewportBbox {

    private ViewportBbox() {}

    public static void validatePoiView(double swLng, double swLat, double neLng, double neLat, double maxBboxDeg) {
        if (!Double.isFinite(swLng)
                || !Double.isFinite(swLat)
                || !Double.isFinite(neLng)
                || !Double.isFinite(neLat)) {
            throw new IllegalArgumentException("bbox values must be finite");
        }
        if (neLat < swLat || neLng < swLng) {
            throw new IllegalArgumentException("bbox must have neLat >= swLat and neLng >= swLng");
        }
        if (neLat - swLat > maxBboxDeg || neLng - swLng > maxBboxDeg) {
            throw new IllegalArgumentException(
                    "POI viewport bbox span may not exceed "
                            + maxBboxDeg
                            + " degrees in latitude and longitude");
        }
    }
}
