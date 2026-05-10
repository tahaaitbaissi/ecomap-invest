package com.example.backend.demographics;

import com.example.backend.entities.Demographics;
import com.uber.h3core.H3Core;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * When {@code app.hexagon.synthetic-demographics-when-missing} is true, fills missing
 * {@link Demographics} fields from the same deterministic synthesis as {@code DemographicsSeeder},
 * so Docker (no {@code dev} profile) still gets population/income for scoring and foot-traffic.
 */
@Component
@RequiredArgsConstructor
public class DeterministicDemographicsFallback {

    private final H3Core h3;

    @Value("${app.hexagon.synthetic-demographics-when-missing:true}")
    private boolean syntheticWhenMissing;

    @Value("${app.h3.demographics.salt:893451263}")
    private long salt;

    @Value("${app.h3.demographics.centre-lat:33.5731}")
    private double centreLat;

    @Value("${app.h3.demographics.centre-lng:-7.5898}")
    private double centreLng;

    public double populationDensity(Optional<Demographics> row, String h3Index) {
        if (row.isPresent() && row.get().getPopulationDensity() != null) {
            return row.get().getPopulationDensity();
        }
        if (!syntheticWhenMissing || h3Index == null || h3Index.isBlank()) {
            return 0.0;
        }
        return DemographicsSynth.forHex(h3Index, h3, salt, centreLat, centreLng).populationDensity();
    }

    public double avgIncome(Optional<Demographics> row, String h3Index) {
        if (row.isPresent() && row.get().getAvgIncome() != null) {
            return row.get().getAvgIncome();
        }
        if (!syntheticWhenMissing || h3Index == null || h3Index.isBlank()) {
            return 0.0;
        }
        return DemographicsSynth.forHex(h3Index, h3, salt, centreLat, centreLng).avgIncome();
    }
}
