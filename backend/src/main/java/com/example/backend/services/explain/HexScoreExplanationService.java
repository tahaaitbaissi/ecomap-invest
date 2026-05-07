package com.example.backend.services.explain;

import com.example.backend.controllers.dto.HexExplanationContextDto;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.output.Response;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Streams a French LLM interpretation of {@link HexExplanationContextDto}; falls back to a template
 * when no streaming model is configured or on errors.
 */
@Slf4j
@Service
public class HexScoreExplanationService {

    private final HexExplanationContextBuilder contextBuilder;

    @Nullable
    private final StreamingChatLanguageModel explainStreamingModel;

    @Autowired
    public HexScoreExplanationService(
            HexExplanationContextBuilder contextBuilder,
            @Autowired(required = false) @Qualifier("explainStreamingChatModel")
                    StreamingChatLanguageModel explainStreamingModel) {
        this.contextBuilder = contextBuilder;
        this.explainStreamingModel = explainStreamingModel;
    }

    /** Runs generation off the request thread; completes or errors the emitter. */
    @Async
    public void streamExplanationAsync(UUID profileId, String h3Index, Integer viewportCellCount, SseEmitter emitter) {
        try {
            HexExplanationContextDto ctx = contextBuilder.build(profileId, h3Index, viewportCellCount);
            String facts = factsPrompt(ctx);
            List<ChatMessage> messages =
                    List.of(
                            SystemMessage.from(systemInstructions()),
                            UserMessage.from(facts));

            if (explainStreamingModel != null) {
                try {
                    explainStreamingModel.generate(
                            messages,
                            new StreamingResponseHandler<>() {
                            @Override
                            public void onNext(String token) {
                                if (token == null || token.isEmpty()) {
                                    return;
                                }
                                try {
                                    emitter.send(SseEmitter.event().data(token));
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            }

                            @Override
                            public void onComplete(Response<dev.langchain4j.data.message.AiMessage> response) {
                                sendDone(emitter);
                            }

                            @Override
                            public void onError(Throwable error) {
                                log.warn("Explain stream failed, using template: {}", error.getMessage());
                                fallbackTemplate(emitter, ctx);
                            }
                        });
                } catch (Exception ex) {
                    log.warn("Explain stream invoke failed, using template: {}", ex.getMessage());
                    fallbackTemplate(emitter, ctx);
                }
            } else {
                fallbackTemplate(emitter, ctx);
            }
        } catch (Exception e) {
            log.warn("Explain setup failed: {}", e.getMessage());
            try {
                emitter.send(
                        SseEmitter.event()
                                .data("Impossible de générer l'analyse : " + e.getMessage()));
            } catch (IOException ignored) {
                // ignore
            }
            safeComplete(emitter);
        }
    }

    private static String systemInstructions() {
        return """
                Répondez exclusivement en français.

                Vous expliquez un score d'implantation pour une cellule H3 Carte (0–100 après normalisation globale).
                INTERDIT d'inventer des points d'intérêt, flux ou chiffres qui ne figurent pas dans les faits JSON fournis.
                Mentionnez la normalisation avec normalizationStretchLow / normalizationStretchHigh (et normalizationFlat si besoin).
                Structurez en 2 à 4 courts paragraphes lisibles (pas de liste JSON en sortie).
                Soyez factuel : citez les tags conducteurs et concurrents fournis et leurs pondérations/contribs agrégées quand utile.
                Si aggregatedFromGridLeaves est true, précisez que les totaux par tag sont la somme sur plusieurs sous-hex présents dans la grille modèle.
                Terminez par une phrase prudente : ce score est indicatif, pas une décision d'investissement seule.
                """;
    }

    static String factsPrompt(HexExplanationContextDto ctx) {
        StringBuilder sb =
                new StringBuilder("Faits JSON pour interprétation (ne pas recopier le JSON dans la réponse utilisateur) :\n");
        sb.append("{\n");
        sb.append("  \"profileName\": \"").append(jsonEscape(ctx.profileName())).append("\",\n");
        sb.append("  \"profileUserQuery\": \"").append(jsonEscape(ctx.profileUserQuery())).append("\",\n");
        sb.append("  \"h3Index\": \"").append(jsonEscape(ctx.h3Index())).append("\",\n");
        sb.append("  \"h3Resolution\": ").append(ctx.h3InputResolution()).append(",\n");
        sb.append("  \"aggregatedFromGridLeaves\": ").append(ctx.aggregatedFromGridLeaves()).append(",\n");
        sb.append("  \"gridLeafResolution\": ").append(ctx.gridLeafResolution()).append(",\n");
        sb.append("  \"gridLeafCount\": ").append(ctx.gridLeafCount()).append(",\n");
        sb.append("  \"interpretationNote\": \"")
                .append(jsonEscape(ctx.aggregationInterpretationNote()))
                .append("\",\n");
        sb.append("  \"center\": {\"lat\": ")
                .append(ctx.centerLat())
                .append(", \"lng\": ")
                .append(ctx.centerLng())
                .append("},\n");
        sb.append("  \"averageRawAcrossLeaves\": ")
                .append(String.format(Locale.US, "%.6f", ctx.averageRawAcrossLeaves()))
                .append(",\n");
        sb.append("  \"normalizationStretchLow\": ")
                .append(String.format(Locale.US, "%.6f", ctx.normalizationStretchLow()))
                .append(",\n");
        sb.append("  \"normalizationStretchHigh\": ")
                .append(String.format(Locale.US, "%.6f", ctx.normalizationStretchHigh()))
                .append(",\n");
        sb.append("  \"normalizationFlat\": ").append(ctx.normalizationFlat()).append(",\n");
        sb.append("  \"computedDisplayScore\": ");
        if (ctx.computedDisplayScore() == null) {
            sb.append("null");
        } else {
            sb.append(String.format(Locale.US, "%.2f", ctx.computedDisplayScore()));
        }
        sb.append(",\n");

        sb.append("  \"summedWeightedDriversAcrossLeaves\": ")
                .append(String.format(Locale.US, "%.4f", ctx.summedWeightedDriversAcrossLeaves()))
                .append(",\n");
        sb.append("  \"summedWeightedCompetitorsAcrossLeaves\": ")
                .append(String.format(Locale.US, "%.4f", ctx.summedWeightedCompetitorsAcrossLeaves()))
                .append(",\n");
        sb.append("  \"averagePopulationTerm\": ")
                .append(String.format(Locale.US, "%.4f", ctx.averagePopulationTerm()))
                .append(",\n");
        sb.append("  \"totalCompetitorPoisUnweightedAcrossLeaves\": ")
                .append(ctx.totalCompetitorPoisUnweightedAcrossLeaves())
                .append(",\n");
        if (ctx.populationDensityAvg() != null) {
            sb.append("  \"populationDensityAvg\": ")
                    .append(String.format(Locale.US, "%.2f", ctx.populationDensityAvg()))
                    .append(",\n");
        }
        if (ctx.avgIncomeAvg() != null) {
            sb.append("  \"avgIncomeAvg\": ")
                    .append(String.format(Locale.US, "%.2f", ctx.avgIncomeAvg()))
                    .append(",\n");
        }
        var snap = ctx.demographics();
        if (snap != null) {
            sb.append("  \"demographicsUsing\": ").append(snap.usingDemographics()).append(",\n");
            if (snap.densityCapUsed() != null) {
                sb.append("  \"demographicDensityCap\": ")
                        .append(String.format(Locale.US, "%.1f", snap.densityCapUsed()))
                        .append(",\n");
            }
        }

        sb.append("  \"drivers\": ");
        appendTags(sb, ctx.drivers());
        sb.append(",\n  \"competitors\": ");
        appendTags(sb, ctx.competitors());
        sb.append("\n}\n");
        return sb.toString();
    }

    private static void appendTags(
            StringBuilder sb, List<HexExplanationContextDto.TagContributionRow> rows) {
        sb.append("[\n");
        for (int i = 0; i < rows.size(); i++) {
            var r = rows.get(i);
            sb.append("    {\"tag\":\"")
                    .append(jsonEscape(r.tag()))
                    .append("\", \"weight\": ")
                    .append(r.weight())
                    .append(", \"countAcrossLeaves\": ")
                    .append(r.countInsideAcrossLeaves())
                    .append(", \"weightedContributionAcrossLeaves\": ")
                    .append(String.format(Locale.US, "%.4f", r.weightedContributionAcrossLeaves()))
                    .append("}");
            if (i < rows.size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
        }
        sb.append("  ]");
    }

    private static String jsonEscape(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "");
    }

    private void fallbackTemplate(SseEmitter emitter, HexExplanationContextDto ctx) {
        String text = deterministicNarrative(ctx);
        for (String chunk : splitForStream(text, 48)) {
            try {
                emitter.send(SseEmitter.event().data(chunk));
            } catch (IOException e) {
                safeComplete(emitter);
                return;
            }
        }
        sendDone(emitter);
    }

    private static List<String> splitForStream(String text, int approxChars) {
        List<String> out = new ArrayList<>();
        String t = text.strip();
        if (t.isEmpty()) {
            return out;
        }
        int start = 0;
        while (start < t.length()) {
            int end = Math.min(t.length(), start + approxChars);
            if (end < t.length()) {
                int sp = t.lastIndexOf(' ', end);
                if (sp > start + 12) {
                    end = sp;
                }
            }
            out.add(t.substring(start, end));
            start = end;
            while (start < t.length() && t.charAt(start) == ' ') {
                start++;
            }
        }
        return out;
    }

    static String deterministicNarrative(HexExplanationContextDto ctx) {
        StringBuilder b = new StringBuilder();
        b.append("**Analyse technique (sans LLM)** — ").append(ctx.profileName()).append('\n').append('\n');
        Double disp = ctx.computedDisplayScore();
        if (disp != null) {
            b.append("Score carte affiché (approx. normalisation grille) ~ **")
                    .append(String.format(Locale.FRANCE, "%.0f", disp))
                    .append("/100**.\n\n");
        } else {
            b.append("Impossible de convertir brut → affiché (distribution plate ou données insuffisantes).\n\n");
        }
        b.append("Brut moyen sur la zone décrite : ")
                .append(String.format(Locale.FRANCE, "%.3f", ctx.averageRawAcrossLeaves()))
                .append(".\nNormalisation utilisée pour l'échelle 0–100 : [" )
                .append(String.format(Locale.FRANCE, "%.3f", ctx.normalizationStretchLow()))
                .append(" … ")
                .append(String.format(Locale.FRANCE, "%.3f", ctx.normalizationStretchHigh()))
                .append("].\n\n");
        b.append(ctx.aggregationInterpretationNote()).append("\n\n");

        if (ctx.drivers() != null && !ctx.drivers().isEmpty()) {
            b.append("Conducteurs agrégés (Σ pondéré sur sous-hex) : ");
            summarizeTags(b, ctx.drivers());
            b.append("\n\n");
        }
        if (ctx.competitors() != null && !ctx.competitors().isEmpty()) {
            b.append("Concurrents agrégés (Σ pondéré) : ");
            summarizeTags(b, ctx.competitors());
            b.append("\n\n");
        }
        b.append("Population/démographie (terme démo moyenne) ~ ")
                .append(String.format(Locale.FRANCE, "%.3f", ctx.averagePopulationTerm()))
                .append(". POI concurrents (non pondérés, somme des sous-cellules) : ")
                .append(ctx.totalCompetitorPoisUnweightedAcrossLeaves())
                .append(".\n\nCeci résume uniquement les chiffres issus du moteur de score.");
        return b.toString();
    }

    private static void summarizeTags(StringBuilder b, List<HexExplanationContextDto.TagContributionRow> rows) {
        boolean first = true;
        for (var r : rows) {
            if (r.countInsideAcrossLeaves() == 0 && r.weightedContributionAcrossLeaves() == 0.0) {
                continue;
            }
            if (!first) {
                b.append("; ");
            }
            first = false;
            b.append(r.tag())
                    .append(": ")
                    .append(r.countInsideAcrossLeaves())
                    .append(" poi × ")
                    .append(String.format(Locale.FRANCE, "%.2f", r.weight()))
                    .append(" → ")
                    .append(String.format(Locale.FRANCE, "%.2f", r.weightedContributionAcrossLeaves()));
        }
    }

    private static void sendDone(SseEmitter emitter) {
        try {
            emitter.send(SseEmitter.event().data("[DONE]"));
        } catch (IOException ignored) {
            // ignore
        }
        safeComplete(emitter);
    }

    private static void safeComplete(SseEmitter emitter) {
        try {
            emitter.complete();
        } catch (Exception ignored) {
            // ignore
        }
    }
}
