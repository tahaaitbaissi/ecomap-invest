package com.example.backend.controllers.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "Partial profile update; password change requires current password")
public record UpdateProfileRequest(
        @Size(min = 2, max = 255) String companyName,
        String currentPassword,
        @Size(min = 8) String newPassword
) {
}
