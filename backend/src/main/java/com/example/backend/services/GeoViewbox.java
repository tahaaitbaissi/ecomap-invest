package com.example.backend.services;

/**
 * Axis-aligned WGS84 view box for geocoding bias: south-west and north-east corners.
 *
 * @param swLng south-west longitude
 * @param swLat south-west latitude
 * @param neLng north-east longitude
 * @param neLat north-east latitude
 */
public record GeoViewbox(double swLng, double swLat, double neLng, double neLat) {}
