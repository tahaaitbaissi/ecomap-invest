package com.example.backend.controllers.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "POI row in zone analytics")
public record TopPoiDto(
        String name,
        String typeTag,
        String address
) {}

