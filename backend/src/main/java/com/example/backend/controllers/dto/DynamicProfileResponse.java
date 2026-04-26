package com.example.backend.controllers.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record DynamicProfileResponse(
        UUID id,
        Long userId,
        String userQuery,
        Instant generatedAt,
        List<TagWeightDto> drivers,
        List<TagWeightDto> competitors
) {}

