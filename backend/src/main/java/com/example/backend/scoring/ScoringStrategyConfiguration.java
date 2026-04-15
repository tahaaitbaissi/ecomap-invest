package com.example.backend.scoring;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class ScoringStrategyConfiguration {

    @Bean
    @Primary
    public ScoringStrategy scoringStrategy(
            @Value("${app.scoring.strategy:local}") String mode,
            @Qualifier("local") ScoringStrategy local,
            @Qualifier("distributed") ScoringStrategy distributed) {
        if ("distributed".equalsIgnoreCase(mode.trim())) {
            return distributed;
        }
        return local;
    }
}
