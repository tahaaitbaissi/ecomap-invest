package com.example.aiorchestrator.services;

import com.example.aiorchestrator.dto.AiOrchestratorChatTurnRequest;
import com.example.aiorchestrator.dto.HexExplanationContextDto;
import com.example.aiorchestrator.services.ConversationMemoryService.ChatTurn;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.output.Response;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatTurnWorkflowService {

    private final StreamingChatLanguageModel model;
    private final ConversationMemoryService memory;
    private final PromptBuilder promptBuilder;

    public void stream(AiOrchestratorChatTurnRequest req, SseEmitter emitter) {
        AtomicBoolean finished = new AtomicBoolean(false);
        var hex = req.context().hex();
        if (hex == null) {
            throw new IllegalArgumentException("context.hex is required");
        }

        String facts = promptBuilder.userFactsPrompt(hex, req.context().opportunity());
        List<ChatMessage> msgs = new ArrayList<>();
        msgs.add(SystemMessage.from(promptBuilder.system()));

        // Include compact memory as prior turns (not raw JSON facts; facts are re-provided each turn).
        for (ChatTurn turn : memory.load(req.conversationId())) {
            if (turn.user() != null && !turn.user().isBlank()) {
                msgs.add(UserMessage.from(turn.user()));
            }
            if (turn.assistant() != null && !turn.assistant().isBlank()) {
                msgs.add(dev.langchain4j.data.message.AiMessage.from(turn.assistant()));
            }
        }

        msgs.add(UserMessage.from(facts + "\n\nQuestion utilisateur: " + req.message()));

        StringBuilder full = new StringBuilder();
        model.generate(
                msgs,
                new StreamingResponseHandler<>() {
                    @Override
                    public void onNext(String token) {
                        if (finished.get()) return;
                        if (token == null || token.isEmpty()) return;
                        full.append(token);
                        try {
                            emitter.send(SseEmitter.event().data(token));
                        } catch (IOException | IllegalStateException e) {
                            // Client disconnected (broken pipe) or emitter already completed.
                            // Stop streaming without escalating to onError (which would try to send more).
                            sendDoneOnce(emitter, finished);
                        }
                    }

                    @Override
                    public void onComplete(Response<dev.langchain4j.data.message.AiMessage> response) {
                        memory.append(req.conversationId(), new ChatTurn(req.message(), full.toString()));
                        sendDoneOnce(emitter, finished);
                    }

                    @Override
                    public void onError(Throwable error) {
                        if (finished.get()) return;
                        log.warn("Chat stream failed: {}", error.getMessage());
                        try {
                            String det = deterministicFallback(req);
                            emitter.send(SseEmitter.event().data(det));
                        } catch (IOException | IllegalStateException ignored) {
                            // ignore
                        }
                        sendDoneOnce(emitter, finished);
                    }
                });
    }

    private static String deterministicFallback(AiOrchestratorChatTurnRequest req) {
        HexExplanationContextDto ctx = req.context().hex();
        StringBuilder b = new StringBuilder();
        b.append("**(Mode déterministe — LLM indisponible)**\n\n");
        b.append("Profil: **").append(ctx.profileName()).append("**\n");

        double low = ctx.normalizationStretchLow();
        double high = ctx.normalizationStretchHigh();
        double raw = ctx.averageRawAcrossLeaves();

        if (ctx.computedDisplayScore() != null) {
            b.append("Heatmap (score affiché): **")
                    .append(String.format(Locale.FRANCE, "%.0f", ctx.computedDisplayScore()))
                    .append("/100** (normalisation globale)\n");
        }
        b.append("Brut moyen (raw): ").append(String.format(Locale.FRANCE, "%.4f", raw)).append("\n");
        b.append("Stretch: [")
                .append(String.format(Locale.FRANCE, "%.3f", low))
                .append(" … ")
                .append(String.format(Locale.FRANCE, "%.3f", high))
                .append("]")
                .append(ctx.normalizationFlat() ? " (plat)" : "")
                .append("\n");
        if (!ctx.normalizationFlat() && high > low) {
            double n = (raw - low) / (high - low);
            double clamped = Math.max(0.0, Math.min(1.0, n));
            b.append("Interprétation: normalisé = clamp((raw - low)/(high-low)) = ")
                    .append(String.format(Locale.FRANCE, "%.3f", clamped))
                    .append(" → ")
                    .append(String.format(Locale.FRANCE, "%.0f", clamped * 100.0))
                    .append("/100\n");
            if (clamped <= 0.000001) {
                b.append("Donc score=0 car raw est au niveau (ou sous) la borne basse de normalisation.\n");
            }
        }
        if (ctx.aggregatedFromGridLeaves()) {
            b.append("Agrégation: Σ sur ").append(ctx.gridLeafCount()).append(" sous-hex (rés. ")
                    .append(ctx.gridLeafResolution()).append(")\n");
        }

        b.append("\nTop conducteurs:\n");
        ctx.drivers().stream()
                .filter(r -> r.countInsideAcrossLeaves() > 0)
                .sorted(Comparator.comparingDouble(HexExplanationContextDto.TagContributionRow::weightedContributionAcrossLeaves).reversed())
                .limit(5)
                .forEach(r ->
                        b.append("- ")
                                .append(r.tag())
                                .append(": ")
                                .append(r.countInsideAcrossLeaves())
                                .append(" × ")
                                .append(String.format(Locale.FRANCE, "%.2f", r.weight()))
                                .append(" → ")
                                .append(String.format(Locale.FRANCE, "%.2f", r.weightedContributionAcrossLeaves()))
                                .append("\n"));
        if (ctx.drivers().stream().noneMatch(r -> r.countInsideAcrossLeaves() > 0)) {
            b.append("- —\n");
        }

        b.append("\nConcurrents (non pondéré): Σ ")
                .append(ctx.totalCompetitorPoisUnweightedAcrossLeaves())
                .append("\n");

        if (req.context().opportunity() != null) {
            b.append("\nOpportunité: présente dans le bundle (détails JSON disponibles côté système), ")
                    .append("mais je ne peux pas l’interpréter sans LLM.\n");
        } else {
            b.append("\nOpportunité: non fournie dans le bundle.\n");
        }

        b.append("\nQuestion: ").append(req.message()).append("\n");
        b.append("Réponse limitée aux faits ci-dessus.\n");
        return b.toString();
    }

    private static void sendDoneOnce(SseEmitter emitter, AtomicBoolean finished) {
        if (!finished.compareAndSet(false, true)) return;
        try {
            emitter.send(SseEmitter.event().data("[DONE]"));
        } catch (IOException | IllegalStateException ignored) {
            // ignore
        }
        try {
            emitter.complete();
        } catch (Exception ignored) {
            // ignore
        }
    }
}

