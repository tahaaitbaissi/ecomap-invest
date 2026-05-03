package com.example.backend.controllers.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "Registered user summary (legacy/alternate flow)")
public record RegisterResponse(
        UUID id,
        String email,
        String companyName,
        String role
) {}

