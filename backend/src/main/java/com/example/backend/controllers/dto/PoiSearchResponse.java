package com.example.backend.controllers.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "POI row for public name/tag search")
public record PoiSearchResponse(
        UUID id,
        String name,
        @JsonProperty("typeTag") String typeTag,
        double lat,
        double lng,
        String address) {}
