package com.example.backend.controllers.dto;

import java.util.UUID;

public record RegisterResponse(
        UUID id,
        String email,
        String companyName,
        String role
) {}

