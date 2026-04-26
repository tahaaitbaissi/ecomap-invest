package com.example.backend.controllers.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GenerateDynamicProfileRequest(
        @NotBlank
        @Size(min = 3, max = 500)
        String query
) {}

