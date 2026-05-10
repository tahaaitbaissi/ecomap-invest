package com.example.backend.config;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OllamaResilienceExecutorConfiguration {

    public static final String OLLAMA_RESILIENCE_EXECUTOR = "ollamaResilienceExecutor";

    @Bean(name = OLLAMA_RESILIENCE_EXECUTOR)
    public Executor ollamaResilienceExecutor() {
        return Executors.newFixedThreadPool(8, Thread.ofPlatform().name("ollama-r4j-", 0L).factory());
    }
}
