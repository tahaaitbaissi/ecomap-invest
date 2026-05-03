package com.example.backend.services;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * Minimal Ollama HTTP client (REST {@code /api/generate}) with Resilience4j (instance {@code ollama}
 * in {@code application.yml}). Model name follows LangChain4j config for consistency.
 */
@Slf4j
@Service
public class OllamaChatClient {

    private final RestClient ollamaRestClient;

    @Value("${langchain4j.ollama.chat-model.model-name:llama3}")
    private String modelName;

    public OllamaChatClient(@Qualifier("ollamaRestClient") RestClient ollamaRestClient) {
        this.ollamaRestClient = ollamaRestClient;
    }

    @CircuitBreaker(name = "ollama", fallbackMethod = "generateFallback")
    @Retry(name = "ollama", fallbackMethod = "generateFallback")
    public String generate(String prompt) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", modelName);
        body.put("prompt", prompt);
        body.put("stream", false);
        return ollamaRestClient
                .post()
                .uri("/api/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(String.class);
    }

    @SuppressWarnings("unused")
    String generateFallback(String prompt, Throwable t) {
        log.warn("Ollama unavailable: {}", t != null ? t.getMessage() : "null");
        return "";
    }
}
