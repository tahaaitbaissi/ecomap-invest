package com.example.backend.config;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiOrchestratorExecutorConfiguration {

    public static final String AI_ORCHESTRATOR_PROBE_EXECUTOR = "aiOrchestratorProbeExecutor";

    @Bean(name = AI_ORCHESTRATOR_PROBE_EXECUTOR)
    public Executor aiOrchestratorProbeExecutor() {
        return Executors.newFixedThreadPool(4, Thread.ofPlatform().name("ai-orch-probe-", 0L).factory());
    }
}
