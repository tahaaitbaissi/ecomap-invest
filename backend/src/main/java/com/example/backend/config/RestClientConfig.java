package com.example.backend.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
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
        ObjectMapper m = new ObjectMapper();
        m.registerModule(new JavaTimeModule());
        m.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        return m;
    }

    @Bean(name = "nominatimExecutor")
    public Executor nominatimExecutor() {
        int pool = Math.max(2, Runtime.getRuntime().availableProcessors());
        return new ThreadPoolExecutor(
                pool,
                pool,
                30L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(512),
                new ThreadPoolExecutor.CallerRunsPolicy());
    }
}
