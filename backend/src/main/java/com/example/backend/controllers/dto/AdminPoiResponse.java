package com.example.backend.controllers.dto;

import java.util.UUID;

public record AdminPoiResponse(
        UUID id,
        String osmId,
        String name,
        String address,
        String typeTag,
        double lat,
        double lng,
        Integer priceLevel,
        Float rating,
        String importedAt) {}

