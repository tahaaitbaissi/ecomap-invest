package com.example.backend.foottraffic.repositories;

import com.example.backend.foottraffic.entities.FootTrafficCellProfile;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface FootTrafficCellProfileRepository extends JpaRepository<FootTrafficCellProfile, String> {

    Optional<FootTrafficCellProfile> findByH3IndexAndScenarioId(String h3Index, int scenarioId);

    Page<FootTrafficCellProfile> findByScenarioId(int scenarioId, Pageable pageable);

    long count();

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query(
            value =
                    """
                    INSERT INTO foot_traffic_cell_profile (
                        h3_index, scenario_id, archetype, archetype_confidence, baseline_daily, peak_hourly,
                        driver_poi_count, competitor_poi_count, transit_poi_count, pop_density, avg_income,
                        noise_seed, computed_at
                    ) VALUES (
                        :h3Index, :scenarioId, :archetype, :archetypeConfidence, :baselineDaily, :peakHourly,
                        :driverPoiCount, :competitorPoiCount, :transitPoiCount, :popDensity, :avgIncome,
                        :noiseSeed, CURRENT_TIMESTAMP
                    )
                    ON CONFLICT (h3_index) DO UPDATE SET
                        scenario_id = EXCLUDED.scenario_id,
                        archetype = EXCLUDED.archetype,
                        archetype_confidence = EXCLUDED.archetype_confidence,
                        baseline_daily = EXCLUDED.baseline_daily,
                        peak_hourly = EXCLUDED.peak_hourly,
                        driver_poi_count = EXCLUDED.driver_poi_count,
                        competitor_poi_count = EXCLUDED.competitor_poi_count,
                        transit_poi_count = EXCLUDED.transit_poi_count,
                        pop_density = EXCLUDED.pop_density,
                        avg_income = EXCLUDED.avg_income,
                        noise_seed = EXCLUDED.noise_seed,
                        computed_at = CURRENT_TIMESTAMP
                    """,
            nativeQuery = true)
    void upsert(
            @Param("h3Index") String h3Index,
            @Param("scenarioId") int scenarioId,
            @Param("archetype") String archetype,
            @Param("archetypeConfidence") double archetypeConfidence,
            @Param("baselineDaily") int baselineDaily,
            @Param("peakHourly") int peakHourly,
            @Param("driverPoiCount") int driverPoiCount,
            @Param("competitorPoiCount") int competitorPoiCount,
            @Param("transitPoiCount") int transitPoiCount,
            @Param("popDensity") double popDensity,
            @Param("avgIncome") double avgIncome,
            @Param("noiseSeed") long noiseSeed);
}
