package com.example.backend.controllers.dto;

import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(min = 2, max = 255) String companyName,
        String currentPassword,
        @Size(min = 8) String newPassword
) {
}
