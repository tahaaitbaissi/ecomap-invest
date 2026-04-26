package com.example.backend.controllers.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record DynamicProfileResponse(
        UUID id,
        UUID userId,
        String userQuery,
        Instant generatedAt,
        List<TagWeightDto> drivers,
        List<TagWeightDto> competitors
) {}

