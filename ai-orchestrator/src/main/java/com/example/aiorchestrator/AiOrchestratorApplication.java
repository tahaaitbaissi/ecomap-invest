package com.example.aiorchestrator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class AiOrchestratorApplication {
    public static void main(String[] args) {
        SpringApplication.run(AiOrchestratorApplication.class, args);
    }
}

