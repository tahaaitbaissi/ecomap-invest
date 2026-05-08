package com.example.backend.foottraffic.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.sql.Timestamp;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "foot_traffic_cell_profile")
public class FootTrafficCellProfile {

    @Id
    @Column(name = "h3_index", length = 15)
    private String h3Index;

    @Column(name = "scenario_id", nullable = false)
    private Integer scenarioId;

    @Column(name = "archetype", length = 32, nullable = false)
    private String archetype;

    @Column(name = "archetype_confidence", nullable = false)
    private Double archetypeConfidence;

    @Column(name = "baseline_daily", nullable = false)
    private Integer baselineDaily;

    @Column(name = "peak_hourly", nullable = false)
    private Integer peakHourly;

    @Column(name = "driver_poi_count", nullable = false)
    private Integer driverPoiCount;

    @Column(name = "competitor_poi_count", nullable = false)
    private Integer competitorPoiCount;

    @Column(name = "transit_poi_count", nullable = false)
    private Integer transitPoiCount;

    @Column(name = "pop_density", nullable = false)
    private Double popDensity;

    @Column(name = "avg_income", nullable = false)
    private Double avgIncome;

    @Column(name = "noise_seed", nullable = false)
    private Long noiseSeed;

    @Column(name = "computed_at", nullable = false)
    private Timestamp computedAt;
}
