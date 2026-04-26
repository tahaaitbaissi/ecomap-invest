package com.example.backend.controllers.dto;

import java.sql.Timestamp;
import java.util.UUID;

public record UserProfileDTO(
        UUID id,
        String email,
        String companyName,
        String role,
        Timestamp createdAt
) {}

