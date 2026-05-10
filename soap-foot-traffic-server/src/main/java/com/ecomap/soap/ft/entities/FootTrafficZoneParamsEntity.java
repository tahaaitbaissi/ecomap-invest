package com.ecomap.soap.ft.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.sql.Timestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Read-only copy of backend {@code foot_traffic_zone_params} (same table in shared Postgres).
 */
@Entity
@Table(name = "foot_traffic_zone_params")
public class FootTrafficZoneParamsEntity {

    @Id
    @Column(name = "archetype", length = 32)
    private String archetype;

    @Column(name = "base_daily_min", nullable = false)
    private Integer baseDailyMin;

    @Column(name = "base_daily_max", nullable = false)
    private Integer baseDailyMax;

    @Column(name = "poi_density_cap", nullable = false)
    private Double poiDensityCap;

    @Column(name = "pop_density_cap", nullable = false)
    private Double popDensityCap;

    @Column(name = "income_weight", nullable = false)
    private Double incomeWeight;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "hourly_curve_wd", columnDefinition = "double precision[24]", nullable = false)
    private Double[] hourlyCurveWd;

    @Column(name = "noise_sigma", nullable = false)
    private Double noiseSigma;

    @Column(name = "updated_at", nullable = false)
    private Timestamp updatedAt;

    public String getArchetype() {
        return archetype;
    }

    public Integer getBaseDailyMin() {
        return baseDailyMin;
    }

    public Integer getBaseDailyMax() {
        return baseDailyMax;
    }

    public Double getPoiDensityCap() {
        return poiDensityCap;
    }

    public Double getPopDensityCap() {
        return popDensityCap;
    }

    public Double getIncomeWeight() {
        return incomeWeight;
    }

    public Double[] getHourlyCurveWd() {
        return hourlyCurveWd;
    }

    public Double getNoiseSigma() {
        return noiseSigma;
    }
}
