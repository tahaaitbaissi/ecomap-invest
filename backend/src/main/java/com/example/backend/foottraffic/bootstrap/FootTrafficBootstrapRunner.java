package com.example.backend.foottraffic.bootstrap;

import com.example.backend.foottraffic.config.FootTrafficProperties;
import com.example.backend.foottraffic.repositories.FootTrafficCellProfileRepository;
import com.example.backend.foottraffic.services.FootTrafficRecomputeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("dev")
@Order(Ordered.HIGHEST_PRECEDENCE + 30)
@RequiredArgsConstructor
public class FootTrafficBootstrapRunner implements CommandLineRunner {

    private final FootTrafficProperties properties;
    private final FootTrafficCellProfileRepository cellProfileRepository;
    private final FootTrafficRecomputeService footTrafficRecomputeService;

    @Override
    public void run(String... args) {
        if (!properties.isEnabled()) {
            return;
        }
        if (cellProfileRepository.count() > 0) {
            log.info("Foot-traffic bootstrap skipped: {} profile row(s) already present", cellProfileRepository.count());
            return;
        }
        log.info("Foot-traffic bootstrap: running initial recompute for empty profile table");
        footTrafficRecomputeService.recomputeAll(0, null);
    }
}
