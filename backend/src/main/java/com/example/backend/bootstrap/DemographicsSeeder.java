package com.example.backend.bootstrap;

import com.example.backend.entities.Demographics;
import com.example.backend.entities.H3Hexagon;
import com.example.backend.repositories.DemographicsRepository;
import com.example.backend.repositories.H3HexagonRepository;
import com.uber.h3core.H3Core;
import com.uber.h3core.util.LatLng;
import java.util.Random;
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
 * Fills {@code demographics} for each H3 index in {@code h3_hexagon} (dev only). Must run
 * after {@link H3GridBootstrapRunner} so the hex table is populated.
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
            Random rnd = new Random(mix64(idx.hashCode(), salt));
            double centre01 = cityCentre01(idx, rnd);
            // Higher density in centre: blend with centre boost
            int low = 1000;
            int high = 18_000;
            double t = rnd.nextDouble();
            double u = (low + t * (high - low)) * (0.5 + 0.5 * centre01);
            double population = Math.clamp(u, low, high);

            double incomeLow = 4000;
            double incomeHigh = 30_000;
            double income =
                    incomeLow + rnd.nextDouble() * (incomeHigh - incomeLow) * (0.85 + 0.15 * centre01);
            income = Math.clamp(income, incomeLow, incomeHigh);

            Demographics d = new Demographics();
            d.setH3Index(idx);
            d.setPopulationDensity(population);
            d.setAvgIncome(income);
            demographicsRepository.save(d);
            added++;
        }
        log.info("Demographics seed: inserted {} row(s) for h3_index cells", added);
    }

    private static long mix64(int h, long s) {
        long x = h ^ s;
        x = (x ^ (x >>> 33)) * 0xff51afd7ed558ccdL;
        x = (x ^ (x >>> 33)) * 0xc4ceb9fe1a85ec53L;
        return x ^ (x >>> 33);
    }

    /** Returns [0,1] with higher values toward Casablanca centre. */
    private double cityCentre01(String h3Index, Random rnd) {
        try {
            long cell = h3.stringToH3(h3Index);
            LatLng c = h3.cellToLatLng(cell);
            double dLat = c.lat - CASA_CENTRE_LAT;
            double dLng = c.lng - CASA_CENTRE_LNG;
            double d2 = dLat * dLat + dLng * dLng;
            return 1.0 - Math.clamp(d2 / 0.0225, 0, 1);
        } catch (Exception e) {
            return 0.3 + 0.7 * rnd.nextDouble();
        }
    }
}
