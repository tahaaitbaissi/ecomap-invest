package com.example.aiorchestrator.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AiOrchestratorChatTurnRequest(
        @NotBlank String conversationId,
        @NotNull AiContextBundle context,
        @NotBlank String message) {}

