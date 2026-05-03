package com.example.backend.scoring;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds {@code app.scoring.*} (see {@code application.yml}). The active {@link ScoringStrategy} is
 * selected in {@link ScoringStrategyConfiguration} from {@code strategy} and the {@code @Qualifier}
 * beans {@code local} and {@code distributed}.
 */
@ConfigurationProperties(prefix = "app.scoring")
public class ScoringProperties {

    /**
     * {@code local} = JVM {@link SaturationFormula}; {@code distributed} = RMI via {@link
     * DistributedScoringStrategy}.
     */
    private String strategy = "local";

    public String getStrategy() {
        return strategy;
    }

    public void setStrategy(String strategy) {
        this.strategy = strategy;
    }
}
