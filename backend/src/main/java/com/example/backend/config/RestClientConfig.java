package com.example.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * RestClient/RestTemplate configuration for HTTP-based external service integrations.
 */
@Configuration
public class RestClientConfig {

    @Bean
    public RestClient geocodingRestClient() {
        return RestClient.builder()
                .baseUrl("https://nominatim.openstreetmap.org/search")
                .defaultHeader("User-Agent", "EcoMapInvest/1.0")
                .build();
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
