package com.example.backend.controllers.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Natural language query to generate tag weights")
public record GenerateDynamicProfileRequest(
        @NotBlank
        @Size(min = 3, max = 500)
        String query
) {}

