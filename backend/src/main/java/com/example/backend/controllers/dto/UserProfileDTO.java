package com.example.backend.controllers.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.sql.Timestamp;
import java.util.UUID;

@Schema(description = "Authenticated user profile")
public record UserProfileDTO(
        UUID id,
        String email,
        String companyName,
        String role,
        Timestamp createdAt
) {}

