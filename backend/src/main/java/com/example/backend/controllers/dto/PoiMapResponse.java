package com.example.backend.controllers.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "POI for map layer with optional saturation score")
public record PoiMapResponse(
        UUID id,
        String name,
        String address,
        String typeTag,
        double latitude,
        double longitude,
        Double saturationScore
) {
}
