package com.example.backend.bootstrap;

import com.example.backend.demographics.DemographicsSynth;
import com.example.backend.entities.Demographics;
import com.example.backend.entities.H3Hexagon;
import com.example.backend.repositories.DemographicsRepository;
import com.example.backend.repositories.H3HexagonRepository;
import com.uber.h3core.H3Core;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Fills {@code demographics} for each H3 index in {@code h3_hexagon} (dev profile). Uses the same
 * synthesis as runtime {@link com.example.backend.demographics.DeterministicDemographicsFallback}.
 */
@Slf4j
@Component
@Profile("dev")
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
@RequiredArgsConstructor
public class DemographicsSeeder implements CommandLineRunner {

    private static final double CASA_CENTRE_LAT = 33.5731;
    private static final double CASA_CENTRE_LNG = -7.5898;

    private final H3Core h3;
    private final H3HexagonRepository h3HexagonRepository;
    private final DemographicsRepository demographicsRepository;

    @Value("${app.h3.demographics.salt:893451263}")
    private long salt;

    @Override
    @Transactional
    public void run(String... args) {
        if (demographicsRepository.count() > 0) {
            log.info("Demographics seed skipped: table already has {} row(s)", demographicsRepository.count());
            return;
        }
        if (h3HexagonRepository.count() == 0) {
            log.warn("Demographics seed skipped: h3_hexagon is empty");
            return;
        }
        int added = 0;
        for (H3Hexagon hex : h3HexagonRepository.findAll()) {
            String idx = hex.getH3Index();
            DemographicsSynth.DensityIncome synth =
                    DemographicsSynth.forHex(idx, h3, salt, CASA_CENTRE_LAT, CASA_CENTRE_LNG);

            Demographics d = new Demographics();
            d.setH3Index(idx);
            d.setPopulationDensity(synth.populationDensity());
            d.setAvgIncome(synth.avgIncome());
            demographicsRepository.save(d);
            added++;
        }
        log.info("Demographics seed: inserted {} row(s) for h3_index cells", added);
    }
}
