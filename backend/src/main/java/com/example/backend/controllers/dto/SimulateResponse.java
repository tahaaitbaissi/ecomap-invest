package com.example.backend.controllers.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Normalized hex scores after applying the hypothetical impact")
public record SimulateResponse(List<HexagonMapResponse> affectedHexagons) {}
