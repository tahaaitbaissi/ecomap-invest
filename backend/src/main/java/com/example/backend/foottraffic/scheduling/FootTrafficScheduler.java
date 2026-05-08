package com.example.backend.foottraffic.scheduling;

import com.example.backend.foottraffic.services.FootTrafficRecomputeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.foot-traffic", name = "enabled", havingValue = "true", matchIfMissing = true)
public class FootTrafficScheduler {

    private final FootTrafficRecomputeService footTrafficRecomputeService;

    @Scheduled(cron = "${app.foot-traffic.recompute.scheduled-cron:0 0 3 * * *}")
    public void nightlyRecompute() {
        try {
            var r = footTrafficRecomputeService.recomputeAll(0, null);
            log.info(
                    "Foot-traffic scheduled recompute done: {} cells, {} ms, v={}",
                    r.cellsProcessed(),
                    r.durationMs(),
                    r.trafficVersion());
        } catch (Exception e) {
            log.warn("Foot-traffic scheduled recompute failed: {}", e.getMessage());
        }
    }
}
