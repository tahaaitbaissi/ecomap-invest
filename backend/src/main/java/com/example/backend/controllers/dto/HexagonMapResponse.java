package com.example.backend.controllers.dto;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * JSON shape must match the frontend {@code HexagonDto} (h3Index, score, boundary lat/lng).
 */
@JsonPropertyOrder({"h3Index", "score", "boundary"})
public record HexagonMapResponse(
        @JsonProperty("h3Index") String h3Index,
        double score,
        List<HexagonMapResponse.LatLng> boundary) {

    @JsonPropertyOrder({"lat", "lng"})
    public record LatLng(
            @JsonProperty("lat") double lat,
            @JsonProperty("lng") double lng) {}
}
