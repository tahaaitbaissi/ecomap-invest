package com.example.backend.services.ai;

import com.example.backend.controllers.dto.HexExplanationContextDto;
import com.example.backend.controllers.dto.OpportunitySimulateRequest;
import com.example.backend.controllers.dto.OpportunitySimulateResponse;
import com.example.backend.services.explain.HexExplanationContextBuilder;
import com.example.backend.services.opportunity.OpportunityScoreService;
import com.example.backend.services.profile.DynamicProfileService;
import com.example.backend.scoring.HexagonRawScoringSupport;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uber.h3core.H3Core;
import java.io.IOException;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatService {

    private final HexExplanationContextBuilder contextBuilder;
    private final DynamicProfileService dynamicProfileService;
    private final OpportunityScoreService opportunityScoreService;
    private final HexagonRawScoringSupport rawScoringSupport;
    private final H3Core h3;
    private final AiOrchestratorClient orchestratorClient;
    private final AiOrchestratorTcpProbe orchestratorProbe;
    private final ObjectMapper objectMapper;

    @Async
    public void streamChatAsync(
            String userEmail,
            UUID profileId,
            String selectedHexIndex,
            Integer viewportCellCount,
            String conversationId,
            String message,
            SseEmitter emitter) {
        try {
            var profile = dynamicProfileService.getOwnedActiveEntity(userEmail, profileId);
            HexExplanationContextDto ctx = contextBuilder.build(profileId, selectedHexIndex, viewportCellCount);

            OpportunitySimulateResponse opp = null;
            try {
                // Optional: used to explain why "opportunity" differs from heatmap.
                // Keep disabled unless explicitly requested (explain=true) to control latency.
                var c = h3.cellToLatLng(h3.stringToH3(selectedHexIndex));
                var cfg = rawScoringSupport.buildConfigForProfile(profileId);
                opp = opportunityScoreService.compute(profile, c.lat, c.lng, cfg, false);
            } catch (Exception e) {
                // non-fatal: opportunity is optional
                log.debug("Opportunity context unavailable: {}", e.getMessage());
            }

            AiContextBundle bundle = new AiContextBundle(ctx, opp);
            AiOrchestratorChatTurnRequest req =
                    new AiOrchestratorChatTurnRequest(conversationId, bundle, message);

            if (orchestratorClient.isEnabled()) {
                try {
                    boolean up = orchestratorProbe.reachableAsync().join();
                    if (up) {
                        orchestratorClient.streamChatTurn(req, emitter);
                        safeComplete(emitter);
                        return;
                    }
                    log.warn("Orchestrator TCP probe failed, using deterministic fallback");
                } catch (Exception e) {
                    log.warn("Orchestrator chat failed, falling back: {}", e.getMessage());
                }
            }

            // Deterministic fallback: send strict, grounded summary + DONE.
            String fallback = deterministicFallback(bundle, message);
            emitter.send(SseEmitter.event().data(fallback));
            emitter.send(SseEmitter.event().data("[DONE]"));
            safeComplete(emitter);
        } catch (Exception e) {
            try {
                emitter.send(SseEmitter.event().data("Impossible de répondre : " + e.getMessage()));
                emitter.send(SseEmitter.event().data("[DONE]"));
            } catch (IOException ignored) {
                // ignore
            }
            safeComplete(emitter);
        }
    }

    private String deterministicFallback(AiContextBundle bundle, String message) {
        HexExplanationContextDto ctx = bundle.hex();
        StringBuilder b = new StringBuilder();
        b.append("**(Mode déterministe — orchestrator indisponible)**\n\n");
        b.append("Profil: **").append(ctx.profileName()).append("**\n");
        double low = ctx.normalizationStretchLow();
        double high = ctx.normalizationStretchHigh();
        double raw = ctx.averageRawAcrossLeaves();
        if (ctx.computedDisplayScore() != null) {
            b.append("Score carte: **")
                    .append(String.format(Locale.FRANCE, "%.0f", ctx.computedDisplayScore()))
                    .append("/100**\n");
        }
        b.append("Brut moyen (raw): ").append(String.format(Locale.FRANCE, "%.4f", raw)).append("\n");
        b.append("Normalisation: [")
                .append(String.format(Locale.FRANCE, "%.3f", low))
                .append(" … ")
                .append(String.format(Locale.FRANCE, "%.3f", high))
                .append("]")
                .append(ctx.normalizationFlat() ? " (plat)" : "")
                .append("\n\n");
        if (!ctx.normalizationFlat() && high > low) {
            double n = (raw - low) / (high - low);
            double clamped = Math.max(0.0, Math.min(1.0, n));
            b.append("Interprétation: clamp((raw-low)/(high-low)) = ")
                    .append(String.format(Locale.FRANCE, "%.3f", clamped))
                    .append(" → ")
                    .append(String.format(Locale.FRANCE, "%.0f", clamped * 100.0))
                    .append("/100\n\n");
        }

        b.append("Top conducteurs (tags):\n");
        ctx.drivers().stream()
                .filter(r -> r.countInsideAcrossLeaves() > 0)
                .sorted((a, c) -> Double.compare(c.weightedContributionAcrossLeaves(), a.weightedContributionAcrossLeaves()))
                .limit(5)
                .forEach(r -> b.append("- ")
                        .append(r.tag())
                        .append(" · ")
                        .append(r.countInsideAcrossLeaves())
                        .append(" × ")
                        .append(String.format(Locale.FRANCE, "%.2f", r.weight()))
                        .append(" → ")
                        .append(String.format(Locale.FRANCE, "%.2f", r.weightedContributionAcrossLeaves()))
                        .append("\n"));
        if (ctx.drivers().stream().noneMatch(r -> r.countInsideAcrossLeaves() > 0)) {
            b.append("- —\n");
        }

        b.append("\nConcurrents (non pondéré): Σ ").append(ctx.totalCompetitorPoisUnweightedAcrossLeaves()).append("\n");

        if (bundle.opportunity() != null) {
            try {
                b.append("\n(Opportunité) Contexte brut:\n");
                b.append(objectMapper.writeValueAsString(bundle.opportunity()));
                b.append("\n");
            } catch (Exception ignored) {
                // ignore
            }
        }

        if (message != null && !message.isBlank()) {
            b.append("\nVotre question: ").append(message.strip()).append("\n");
            b.append("Réponse limitée aux faits ci-dessus (LLM non disponible).\n");
        }
        return b.toString();
    }

    private static void safeComplete(SseEmitter emitter) {
        try {
            emitter.complete();
        } catch (Exception ignored) {
            // ignore
        }
    }
}

