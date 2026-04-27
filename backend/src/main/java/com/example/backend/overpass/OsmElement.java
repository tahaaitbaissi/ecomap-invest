package com.example.backend.overpass;

import java.util.Map;

/**
 * Minimal OSM element from Overpass JSON (node with lat/lon).
 */
public record OsmElement(long id, double lat, double lon, Map<String, String> tags, String sourceTag) {
}
