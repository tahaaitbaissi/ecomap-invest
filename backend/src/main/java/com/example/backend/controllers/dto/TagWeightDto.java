package com.example.backend.controllers.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TagWeightDto(
        @NotBlank
        @Size(max = 200)
        String tag,

        @NotNull
        Double weight
) {}

