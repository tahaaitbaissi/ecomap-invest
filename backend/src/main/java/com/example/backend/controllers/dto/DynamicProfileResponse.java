package com.example.backend.controllers.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(description = "Persisted dynamic profile with driver/competitor tag weights")
public record DynamicProfileResponse(
        UUID id,
        UUID userId,
        String userQuery,
        Instant generatedAt,
        List<TagWeightDto> drivers,
        List<TagWeightDto> competitors
) {}

