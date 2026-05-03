package com.example.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class OllamaConfiguration {

    @Bean(name = "ollamaRestClient")
    public RestClient ollamaRestClient(
            @Value("${langchain4j.ollama.chat-model.base-url:http://localhost:11434}") String baseUrl) {
        String base = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        return RestClient.builder().baseUrl(base).build();
    }
}
