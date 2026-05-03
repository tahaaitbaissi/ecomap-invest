package com.example.backend.controllers.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Email and password for login")
public record LoginRequest(
        @Email @NotBlank String email,
        @NotBlank String password
) {
}
