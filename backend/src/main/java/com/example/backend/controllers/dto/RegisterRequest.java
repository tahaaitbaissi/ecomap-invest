package com.example.backend.controllers.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "New investor registration")
public record RegisterRequest(
        @Email @NotBlank String email,
        @Size(min = 8)
        @Pattern(regexp = "(?=.*[A-Z])(?=.*[a-z])(?=.*\\d).*")
        @NotBlank String password,
        @NotBlank String companyName
) {
}
