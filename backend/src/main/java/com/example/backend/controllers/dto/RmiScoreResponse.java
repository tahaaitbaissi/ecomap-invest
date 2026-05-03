package com.example.backend.controllers.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Score from RMI demo endpoint")
public record RmiScoreResponse(double score, String source) {
}
