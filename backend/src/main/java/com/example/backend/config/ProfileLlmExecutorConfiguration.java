package com.example.backend.config;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ProfileLlmExecutorConfiguration {

    public static final String PROFILE_LLM_EXECUTOR = "profileLlmExecutor";

    @Bean(name = PROFILE_LLM_EXECUTOR)
    public Executor profileLlmExecutor() {
        return Executors.newFixedThreadPool(8, Thread.ofPlatform().name("profile-llm-r4j-", 0L).factory());
    }
}
