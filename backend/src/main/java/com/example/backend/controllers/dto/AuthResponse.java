package com.example.backend.controllers.dto;

public record AuthResponse(
        String accessToken,
        String tokenType
) {
}
