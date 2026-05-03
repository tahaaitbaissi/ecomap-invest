package com.example.backend.scoring;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * <p>Strategy pattern for saturation scoring: two {@link ScoringStrategy} implementations are
 * registered with {@code @Qualifier("local")} and {@code @Qualifier("distributed")}. The active
 * strategy is the {@code @Primary} bean, chosen from {@link ScoringProperties#getStrategy()} (YAML
 * {@code app.scoring.strategy}, env {@code SCORING_STRATEGY}).
 *
 * <p>Callers such as {@link com.example.backend.services.PoiService} inject {@link ScoringStrategy}
 * without a qualifier and receive the configured implementation.
 */
@Configuration
@EnableConfigurationProperties(ScoringProperties.class)
public class ScoringStrategyConfiguration {

    @Bean
    @Primary
    public ScoringStrategy scoringStrategy(
            ScoringProperties properties,
            @Qualifier("local") ScoringStrategy local,
            @Qualifier("distributed") ScoringStrategy distributed) {
        String mode = properties.getStrategy() != null ? properties.getStrategy().trim() : "local";
        if ("distributed".equalsIgnoreCase(mode)) {
            return distributed;
        }
        return local;
    }
}
