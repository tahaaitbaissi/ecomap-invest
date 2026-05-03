package com.example.backend.controllers.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "JWT and basic user attributes")
public record AuthResponse(
        String token,
        long expiresIn,
        String userEmail,
        String role
) {
}
