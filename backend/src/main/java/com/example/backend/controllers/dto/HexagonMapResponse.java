package com.example.backend.controllers.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.List;

/**
 * JSON shape matches the frontend {@code HexagonDto}. {@code score} is null when no investment
 * profile is selected (gray-only map mode).
 */
@JsonPropertyOrder({"h3Index", "score", "boundary"})
@Schema(description = "H3 cell with optional score 0-100 and boundary ring")
public record HexagonMapResponse(
        @JsonProperty("h3Index") String h3Index,
        @JsonInclude(JsonInclude.Include.ALWAYS) @JsonProperty("score") Double score,
        List<HexagonMapResponse.LatLng> boundary) {

    @JsonPropertyOrder({"lat", "lng"})
    @Schema(description = "WGS84 vertex")
    public record LatLng(
            @JsonProperty("lat") double lat,
            @JsonProperty("lng") double lng) {}
}
