package com.example.backend.services.ai;

import com.example.backend.controllers.dto.HexExplanationContextDto;
import com.example.backend.controllers.dto.OpportunitySimulateResponse;

/**
 * Context that the AI assistant is allowed to use for a single chat turn.
 * This is built by the backend (authoritative) and forwarded to the orchestrator (untrusted).
 */
public record AiContextBundle(
        HexExplanationContextDto hex,
        OpportunitySimulateResponse opportunity) {}

