package com.example.backend.controllers.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Nominatim search suggestion with optional result bounding box")
public record GeocodingSuggestionResponse(
        @JsonProperty("displayName") String displayName,
        double lat,
        double lng,
        @JsonProperty("southLat") Double southLat,
        @JsonProperty("westLng") Double westLng,
        @JsonProperty("northLat") Double northLat,
        @JsonProperty("eastLng") Double eastLng,
        @JsonProperty("osmType") String osmType,
        @JsonProperty("osmCategory") String osmCategory) {}
