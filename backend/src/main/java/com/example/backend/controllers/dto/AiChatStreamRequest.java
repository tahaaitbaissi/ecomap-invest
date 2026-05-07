package com.example.backend.controllers.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AiChatStreamRequest(
        @NotBlank String conversationId,
        @NotNull UUID profileId,
        @NotBlank String selectedHexIndex,
        Integer viewportCellCount,
        @NotBlank String message) {}

