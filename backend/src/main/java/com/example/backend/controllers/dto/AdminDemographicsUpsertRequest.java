package com.example.backend.controllers.dto;

import jakarta.validation.constraints.NotNull;

public record AdminDemographicsUpsertRequest(
        @NotNull Double populationDensity,
        @NotNull Double avgIncome) {}

