package com.example.backend.controllers.dto;

public record AdminDemographicsResponse(
        String h3Index,
        Double populationDensity,
        Double avgIncome,
        String lastUpdated) {}

