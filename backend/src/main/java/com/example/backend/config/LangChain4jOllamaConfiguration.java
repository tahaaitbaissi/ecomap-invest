package com.example.backend.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * LangChain4j Ollama {@link ChatLanguageModel} (explicit beans; no LangChain4j Spring Boot starter —
 * compatibility with Spring Boot 4). Wired from {@code langchain4j.ollama.chat-model.*} and {@code
 * app.profile.llm.temperature} for deterministic JSON generation.
 */
@Configuration
public class LangChain4jOllamaConfiguration {

    /**
     * Primary model for profile JSON generation (system prompt produces strict JSON).
     */
    @Bean(name = "profileChatModel")
    public ChatLanguageModel profileChatModel(
            @Value("${langchain4j.ollama.chat-model.base-url:http://localhost:11434}") String baseUrl,
            @Value("${langchain4j.ollama.chat-model.model-name:llama3}") String modelName,
            @Value("${app.profile.llm.temperature:0.0}") double profileTemperature) {
        String base = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        return OllamaChatModel.builder()
                .baseUrl(base)
                .modelName(modelName)
                .temperature(profileTemperature)
                .build();
    }
}
