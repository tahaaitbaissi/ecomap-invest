package com.example.backend.controllers.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

@Schema(description = "Business-specific opportunity what-if at a lat/lng for a profile")
public record OpportunitySimulateRequest(
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double lat,
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double lng,
        @NotNull UUID profileId,
        @Schema(description = "Ask LLM for a short textual summary (numbers stay deterministic)") boolean explain) {}
