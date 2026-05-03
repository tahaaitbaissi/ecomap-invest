package com.example.backend.scoring;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.backend.services.rmi.RmiScoringClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

class ScoringStrategyConfigurationTest {

    private final ApplicationContextRunner runner =
            new ApplicationContextRunner().withUserConfiguration(TestScoringSlice.class);

    @Test
    void primaryBeanIsLocalWhenStrategyDefault() {
        runner.run(
                ctx -> {
                    assertThat(ctx.getBeansOfType(ScoringStrategy.class)).hasSize(3);
                    assertThat(ctx.getBean(ScoringStrategy.class)).isInstanceOf(LocalScoringStrategy.class);
                });
    }

    @Test
    void primaryBeanIsLocalWhenStrategyBlankUsesLocal() {
        runner
                .withPropertyValues("app.scoring.strategy=  ")
                .run(
                        ctx ->
                                assertThat(ctx.getBean(ScoringStrategy.class))
                                        .isInstanceOf(LocalScoringStrategy.class));
    }

    @Test
    void primaryBeanIsDistributedWhenConfigured() {
        runner
                .withPropertyValues("app.scoring.strategy=distributed")
                .run(
                        ctx ->
                                assertThat(ctx.getBean(ScoringStrategy.class))
                                        .isInstanceOf(DistributedScoringStrategy.class));
    }

    @Test
    void strategyIsCaseInsensitive() {
        runner
                .withPropertyValues("app.scoring.strategy=DiStRiBuTeD")
                .run(
                        ctx ->
                                assertThat(ctx.getBean(ScoringStrategy.class))
                                        .isInstanceOf(DistributedScoringStrategy.class));
    }

    /** Minimal slice: scoring beans + mock RMI client (no DB). */
    @Configuration
    @Import({LocalScoringStrategy.class, DistributedScoringStrategy.class, ScoringStrategyConfiguration.class})
    static class TestScoringSlice {

        @Bean
        RmiScoringClient rmiScoringClient() {
            return org.mockito.Mockito.mock(RmiScoringClient.class);
        }
    }
}
