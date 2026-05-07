package com.example.backend.services.ai;

public record AiOrchestratorChatTurnRequest(
        String conversationId,
        AiContextBundle context,
        String message) {}

