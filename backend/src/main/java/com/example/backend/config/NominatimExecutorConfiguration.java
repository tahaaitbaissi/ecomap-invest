package com.example.backend.config;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NominatimExecutorConfiguration {

    public static final String NOMINATIM_RESILIENCE_EXECUTOR = "nominatimResilienceExecutor";

    @Bean(name = NOMINATIM_RESILIENCE_EXECUTOR)
    public Executor nominatimResilienceExecutor() {
        return Executors.newFixedThreadPool(8, Thread.ofPlatform().name("nominatim-r4j-", 0L).factory());
    }
}
