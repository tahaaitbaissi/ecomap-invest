package com.example.backend.controllers.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "OSM-style tag with relative weight")
public record TagWeightDto(
        @NotBlank
        @Size(max = 200)
        String tag,

        @NotNull
        Double weight
) {}

