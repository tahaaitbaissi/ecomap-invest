package com.example.backend.services.opportunity;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

/** Optional 2–3 sentence explanation grounded in deterministic numeric facts. */
@Slf4j
@Service
public class OpportunityExplanationService {

    @Nullable
    private final ChatLanguageModel explainModel;

    public OpportunityExplanationService(
            @Autowired(required = false) @Qualifier("profileChatModel") ChatLanguageModel explainModel) {
        this.explainModel = explainModel;
    }

    public Optional<String> explain(
            String profileName,
            double opportunityScore,
            double demand,
            double competitionPenalty,
            double cluster,
            double fitBonus,
            long competitorNearby,
            long competitorInHex,
            String archetypeId) {
        if (explainModel == null) {
            return Optional.empty();
        }
        try {
            String facts =
                    String.format(
                            "Business profile: \"%s\". Archetype rules: %s. "
                                    + "Opportunity score %.1f/100. Demand %.1f, competition penalty %.1f, cluster adjustment +%.1f, surroundings fit +%.1f. "
                                    + "Competitor POI count (profile tags): %d within ~500 m radius, %d inside the same H3 hex "
                                    + "(saturation bands use max of those).",
                            profileName,
                            archetypeId,
                            opportunityScore,
                            demand,
                            competitionPenalty,
                            cluster,
                            fitBonus,
                            competitorNearby,
                            competitorInHex);
            List<ChatMessage> messages =
                    List.of(
                            SystemMessage.from(
                                    "Répondez en 2–3 phrases maximum (français). "
                                            + "Interprétez UNIQUEMENT les faits et chiffres fournis. "
                                            + "Ne changez pas les valeurs numériques."),
                            UserMessage.from(facts));
            Response<AiMessage> res = explainModel.generate(messages);
            if (res == null || res.content() == null) {
                return Optional.empty();
            }
            String text = res.content().text();
            if (text == null || text.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(text.trim());
        } catch (Exception e) {
            log.warn("Opportunity LLM explanation failed: {}", e.getMessage());
            return Optional.empty();
        }
    }
}
