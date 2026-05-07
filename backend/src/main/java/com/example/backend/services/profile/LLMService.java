package com.example.backend.services.profile;

import com.example.backend.controllers.dto.TagWeightDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * F7.1.2: Ollama (via LangChain4j {@link ChatLanguageModel}) returns a single JSON object; this
 * service validates it and maps to {@link FallbackProfileProvider.ProfileConfig}. F7.1.3: covered
 * by Resilience4j + {@link DynamicProfileGenerationService} using {@link FallbackProfileProvider} on
 * empty result.
 */
@Service
@Slf4j
public class LLMService {

    private static final Pattern JSON_FENCE =
            Pattern.compile("```(?:json)?\\s*([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);
    /** OSM-style {@code key=value} (lowercase key part, flexible value). */
    private static final Pattern TAG_FORM = Pattern.compile("^[a-z0-9_]+=[^\\s]+");

    private final ChatLanguageModel profileChatModel;
    private final ObjectMapper objectMapper;
    private final ProfileTagCatalog profileTagCatalog;

    public LLMService(
            @Qualifier("profileChatModel") ChatLanguageModel profileChatModel,
            ObjectMapper objectMapper,
            ProfileTagCatalog profileTagCatalog) {
        this.profileChatModel = profileChatModel;
        this.objectMapper = objectMapper;
        this.profileTagCatalog = profileTagCatalog;
    }

    @Retry(name = "profileLlm")
    @CircuitBreaker(name = "profileLlm", fallbackMethod = "generateProfileConfigFallback")
    public Optional<FallbackProfileProvider.ProfileConfig> generateProfileConfig(String userQuery) {
        return Optional.of(parseStrictJsonProfile(userQuery));
    }

    @SuppressWarnings("unused")
    Optional<FallbackProfileProvider.ProfileConfig> generateProfileConfigFallback(String userQuery, Throwable t) {
        log.warn(
                "Profile LLM unavailable (circuit or error) for query prefix [{}]: {}",
                userQuery == null || userQuery.length() < 64 ? userQuery : userQuery.substring(0, 64) + "…",
                t != null ? t.getClass().getSimpleName() + ": " + t.getMessage() : "null");
        return Optional.empty();
    }

    private FallbackProfileProvider.ProfileConfig parseStrictJsonProfile(String userQuery) {
        if (userQuery == null || userQuery.isBlank()) {
            throw new IllegalArgumentException("userQuery must not be blank");
        }
        List<ChatMessage> messages =
                List.of(
                        SystemMessage.from(systemPrompt()),
                        UserMessage.from("Context: " + userQuery.trim()));
        Response<AiMessage> res = profileChatModel.generate(messages);
        if (res == null || res.content() == null) {
            throw new IllegalStateException("empty model response");
        }
        String text = res.content().text();
        if (text == null || text.isBlank()) {
            throw new IllegalStateException("empty model text");
        }
        String json = extractTopLevelJsonObject(text);
        JsonNode root;
        try {
            root = objectMapper.readTree(json);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("invalid JSON: " + e.getMessage(), e);
        }
        if (root == null || !root.isObject()) {
            throw new IllegalArgumentException("root must be a JSON object");
        }
        List<TagWeightDto> drivers = readTagArray(root.get("drivers"), "drivers");
        List<TagWeightDto> competitors = readTagArray(root.get("competitors"), "competitors");
        if (drivers.isEmpty() || competitors.isEmpty()) {
            throw new IllegalArgumentException("drivers and competitors must be non-empty arrays");
        }
        return new FallbackProfileProvider.ProfileConfig(drivers, competitors);
    }

    String systemPrompt() {
        return """
                You output only one JSON object, no markdown fences, no commentary.
                The user describes a business or investment context for a location-scoring map.

                Required JSON schema:
                {"drivers":[{"tag":"supported_tag","weight":1.0}],"competitors":[{"tag":"supported_tag","weight":1.0}]}

                Rules:
                - Use ONLY tags from the supported catalog below. Do not invent new tags.
                - drivers are demand or foot-traffic anchors that increase opportunity near a hex.
                - competitors are same-offering or substitute businesses that reduce opportunity.
                - If the exact business type is not supported, choose the closest supported proxy instead of inventing a tag.
                - weight is a number from 0.1 to 1.5: 0.1 weak, 1.0 normal, 1.5 strong.
                - Include 3 to 6 drivers and 1 to 3 competitors when possible.
                - Prefer tags that are likely to exist in Casablanca POI data.

                Supported catalog:
                %s

                Examples:
                Cafe -> {"drivers":[{"tag":"office=company","weight":1.1},{"tag":"amenity=university","weight":0.9},{"tag":"leisure=park","weight":0.6}],"competitors":[{"tag":"amenity=cafe","weight":1.2},{"tag":"amenity=restaurant","weight":0.7}]}
                Snack or restaurant -> {"drivers":[{"tag":"office=company","weight":1.0},{"tag":"amenity=university","weight":0.9},{"tag":"shop=supermarket","weight":0.6}],"competitors":[{"tag":"amenity=restaurant","weight":1.2},{"tag":"amenity=cafe","weight":0.6}]}
                Pharmacy -> {"drivers":[{"tag":"amenity=hospital","weight":1.2},{"tag":"office=company","weight":0.6},{"tag":"amenity=school","weight":0.5}],"competitors":[{"tag":"amenity=pharmacy","weight":1.2},{"tag":"shop=supermarket","weight":0.5}]}
                Lawyer or notary -> {"drivers":[{"tag":"office=company","weight":1.2},{"tag":"amenity=bank","weight":0.8},{"tag":"amenity=university","weight":0.4}],"competitors":[{"tag":"office=company","weight":0.6}]}
                """.formatted(profileTagCatalog.promptCatalog());
    }

    private static String extractTopLevelJsonObject(String raw) {
        String t = raw.trim();
        var m = JSON_FENCE.matcher(t);
        if (m.find()) {
            t = m.group(1).trim();
        }
        int start = t.indexOf('{');
        int end = t.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new IllegalArgumentException("no JSON object in model output");
        }
        return t.substring(start, end + 1);
    }

    private static List<TagWeightDto> readTagArray(JsonNode arrayNode, String fieldName) {
        if (arrayNode == null || !arrayNode.isArray()) {
            throw new IllegalArgumentException("missing or invalid array: " + fieldName);
        }
        List<TagWeightDto> out = new ArrayList<>();
        for (JsonNode el : arrayNode) {
            if (el == null || !el.isObject()) {
                continue;
            }
            JsonNode tagNode = el.get("tag");
            JsonNode weightNode = el.get("weight");
            if (tagNode == null || !tagNode.isTextual()) {
                throw new IllegalArgumentException(fieldName + " entry needs textual tag");
            }
            String tag = tagNode.asText().trim();
            if (!TAG_FORM.matcher(tag).matches()) {
                throw new IllegalArgumentException("invalid tag format: " + tag);
            }
            if (weightNode == null || !weightNode.isNumber()) {
                throw new IllegalArgumentException("weight must be numeric for tag " + tag);
            }
            double w = weightNode.asDouble();
            if (!Double.isFinite(w)) {
                throw new IllegalArgumentException("invalid weight for tag " + tag);
            }
            out.add(new TagWeightDto(tag, w));
        }
        return out;
    }
}
