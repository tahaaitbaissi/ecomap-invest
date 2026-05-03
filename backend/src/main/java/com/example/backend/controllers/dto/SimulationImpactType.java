package com.example.backend.controllers.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Whether the hypothetical POI is a driver or competitor")
public enum SimulationImpactType {
    DRIVER,
    COMPETITOR
}
