package com.example.backend.seeder;

import com.example.backend.entities.Demographics;
import com.example.backend.entities.H3Hexagon;
import com.example.backend.repositories.DemographicsRepository;
import com.example.backend.repositories.H3HexagonRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Component
@Profile("!test")
@Order(2)
public class DemographicsSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemographicsSeeder.class);

    private final H3HexagonRepository h3HexagonRepository;
    private final DemographicsRepository demographicsRepository;

    public DemographicsSeeder(H3HexagonRepository h3HexagonRepository,
                               DemographicsRepository demographicsRepository) {
        this.h3HexagonRepository = h3HexagonRepository;
        this.demographicsRepository = demographicsRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        if (demographicsRepository.count() > 0) {
            log.info("Demographics already seeded. Skipping.");
            return;
        }

        log.info("Seeding demographics for all H3 hexagons...");

        List<H3Hexagon> hexagons = h3HexagonRepository.findAll();
        List<Demographics> demographicsList = new ArrayList<>();

        for (H3Hexagon hexagon : hexagons) {
            // Seed reproductible basé sur h3Index
            Random random = new Random(hexagon.getH3Index().hashCode());

            // Centre-ville : densité et revenu plus élevés
            boolean isCentreVille = isCentreVille(hexagon.getH3Index());

            double populationDensity = isCentreVille
                ? 8000 + random.nextDouble() * 10000   // 8000-18000
                : 1000 + random.nextDouble() * 7000;   // 1000-8000

            double avgIncome = isCentreVille
                ? 15000 + random.nextDouble() * 15000  // 15000-30000 MAD
                : 4000 + random.nextDouble() * 11000;  // 4000-15000 MAD

            Demographics demo = new Demographics();
            demo.setH3Index(hexagon.getH3Index());
            demo.setPopulationDensity(populationDensity);
            demo.setAvgIncome(avgIncome);

            demographicsList.add(demo);
        }

        demographicsRepository.saveAll(demographicsList);
        log.info("Demographics seeded for {} hexagons.", demographicsList.size());
    }

    private boolean isCentreVille(String h3Index) {
        // Les hexagones du centre ont des hashcodes plus élevés (approximation)
        return Math.abs(h3Index.hashCode()) % 5 == 0;
    }
}
