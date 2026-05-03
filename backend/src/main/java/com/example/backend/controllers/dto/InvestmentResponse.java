package com.example.backend.controllers.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.UUID;

@Schema(description = "Persisted investment")
public record InvestmentResponse(
        UUID id, UUID userId, BigDecimal amount, String currency, String note, Timestamp createdAt) {}
