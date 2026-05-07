package com.example.backend.controllers.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.util.List;

public record UpdateDynamicProfileRequest(
        @Size(min = 1, max = 120)
        String name,

        @Valid
        List<TagWeightDto> drivers,

        @Valid
        List<TagWeightDto> competitors
) {}
