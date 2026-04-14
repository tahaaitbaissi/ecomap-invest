package com.example.backend.overpass;

/**
 * Geographic bounding box in WGS84, using Overpass API order: south, west, north, east.
 */
public record BoundingBox(double south, double west, double north, double east) {

    public BoundingBox {
        if (south < -90 || south > 90 || north < -90 || north > 90) {
            throw new IllegalArgumentException("latitude must be in [-90, 90]");
        }
        if (west < -180 || west > 180 || east < -180 || east > 180) {
            throw new IllegalArgumentException("longitude must be in [-180, 180]");
        }
        if (south > north) {
            throw new IllegalArgumentException("south must be <= north");
        }
        if (west > east) {
            throw new IllegalArgumentException("west must be <= east");
        }
    }
}
