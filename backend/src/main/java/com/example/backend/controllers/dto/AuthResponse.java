package com.example.backend.controllers.dto;

public record AuthResponse(
        String token,
        long expiresIn,
        String userEmail,
        String role
) {
}
