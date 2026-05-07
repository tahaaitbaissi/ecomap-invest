package com.example.aiorchestrator.config;

import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LangChain4jOllamaConfiguration {

    @Bean
    public StreamingChatLanguageModel chatStreamingModel(
            @Value("${langchain4j.ollama.chat-model.base-url:http://localhost:11434}") String baseUrl,
            @Value("${langchain4j.ollama.chat-model.model-name:llama3}") String modelName,
            @Value("${app.ai.llm.temperature:0.3}") double temperature,
            @Value("${app.ai.llm.timeout:PT180S}") Duration timeout) {
        String base = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        return OllamaStreamingChatModel.builder()
                .baseUrl(base)
                .modelName(modelName)
                .temperature(temperature)
                .timeout(timeout)
                .build();
    }
}

