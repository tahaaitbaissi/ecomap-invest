package com.example.backend.seeder;

import com.example.backend.entities.Demographics;
import com.example.backend.entities.H3Hexagon;
import com.example.backend.repositories.DemographicsRepository;
import com.example.backend.repositories.H3HexagonRepository;
import com.example.backend.services.H3GridService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Random;

@Component
@Profile("!test")
public class H3InitializationRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(H3InitializationRunner.class);

    private final H3GridService h3GridService;
    private final H3HexagonRepository h3HexagonRepository;
    private final DemographicsRepository demographicsRepository;

    public H3InitializationRunner(H3GridService h3GridService,
                                   H3HexagonRepository h3HexagonRepository,
                                   DemographicsRepository demographicsRepository) {
        this.h3GridService = h3GridService;
        this.h3HexagonRepository = h3HexagonRepository;
        this.demographicsRepository = demographicsRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        if (h3HexagonRepository.count() > 0) {
            log.info("H3 hexagons already initialized. Skipping.");
            return;
        }

        log.info("Initializing H3 hexagon grid for Casablanca...");

        List<H3Hexagon> hexagons = h3GridService.generateHexagonsForBBox(
            33.35, -7.85, 33.75, -7.25, 9
        );

        int chunkSize = 500;
        for (int i = 0; i < hexagons.size(); i += chunkSize) {
            int end = Math.min(i + chunkSize, hexagons.size());
            List<H3Hexagon> chunk = hexagons.subList(i, end);
            h3HexagonRepository.saveAll(chunk);
            seedDemographics(chunk);
            log.info("Saved hexagons and demographics {}/{}", end, hexagons.size());
        }

        log.info("H3 grid and demographics initialized with {} units.", hexagons.size());
    }

    private void seedDemographics(List<H3Hexagon> hexagons) {
        Random random = new Random();
        for (H3Hexagon hex : hexagons) {
            Demographics demo = new Demographics();
            demo.setH3Index(hex.getH3Index());
            demo.setPopulationDensity(1000 + (18000 - 1000) * random.nextDouble());
            demo.setAvgIncome(4000 + (30000 - 4000) * random.nextDouble());
            demographicsRepository.save(demo);
        }
    }
}