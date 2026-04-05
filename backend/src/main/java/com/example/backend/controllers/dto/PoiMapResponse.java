package com.example.backend.controllers.dto;

import java.util.UUID;

public record PoiMapResponse(
        UUID id,
        String name,
        String address,
        String typeTag,
        double latitude,
        double longitude
) {
}
