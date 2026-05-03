package com.example.backend.bootstrap;

import com.example.backend.services.H3GridService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Runs {@link H3GridService#generateAndPersistCasablancaIfEmpty()} at startup when seeding is
 * enabled.
 */
@Slf4j
@Component
@Profile("!test")
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@ConditionalOnProperty(prefix = "app.h3.seed", name = "enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class H3GridBootstrapRunner implements CommandLineRunner {

    private final H3GridService h3GridService;

    @Override
    public void run(String... args) {
        h3GridService.generateAndPersistCasablancaIfEmpty();
    }
}
