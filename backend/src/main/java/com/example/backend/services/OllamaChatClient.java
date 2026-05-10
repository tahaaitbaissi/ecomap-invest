package com.example.backend.services;

import com.example.backend.config.OllamaResilienceExecutorConfiguration;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * Minimal Ollama HTTP client (REST {@code /api/generate}) with Resilience4j (circuit breaker, retry,
 * time limiter) on {@link #generateAsync}.
 */
@Slf4j
@Service
public class OllamaChatClient {

    private final RestClient ollamaRestClient;
    private final Executor ollamaExecutor;

    @Value("${langchain4j.ollama.chat-model.model-name:llama3}")
    private String modelName;

    public OllamaChatClient(
            @Qualifier("ollamaRestClient") RestClient ollamaRestClient,
            @Qualifier(OllamaResilienceExecutorConfiguration.OLLAMA_RESILIENCE_EXECUTOR) Executor ollamaExecutor) {
        this.ollamaRestClient = ollamaRestClient;
        this.ollamaExecutor = ollamaExecutor;
    }

    public String generate(String prompt) {
        return generateAsync(prompt).join();
    }

    @CircuitBreaker(name = "ollama", fallbackMethod = "generateAsyncFallback")
    @Retry(name = "ollama", fallbackMethod = "generateAsyncFallback")
    @TimeLimiter(name = "ollama", fallbackMethod = "generateAsyncFallback")
    public CompletableFuture<String> generateAsync(String prompt) {
        return CompletableFuture.supplyAsync(() -> doGenerate(prompt), ollamaExecutor);
    }

    private String doGenerate(String prompt) {
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
    private CompletableFuture<String> generateAsyncFallback(String prompt, Throwable t) {
        log.warn("Ollama unavailable: {}", t != null ? t.getMessage() : "null");
        return CompletableFuture.completedFuture("");
    }
}
