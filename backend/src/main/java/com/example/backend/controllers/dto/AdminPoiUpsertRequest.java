package com.example.backend.controllers.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AdminPoiUpsertRequest(
        String osmId,
        String name,
        String address,
        @NotBlank String typeTag,
        @NotNull Double lat,
        @NotNull Double lng,
        Integer priceLevel,
        Float rating) {}

