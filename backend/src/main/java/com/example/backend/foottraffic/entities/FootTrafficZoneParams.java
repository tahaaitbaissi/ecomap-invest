package com.example.backend.foottraffic.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.sql.Timestamp;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@Entity
@Table(name = "foot_traffic_zone_params")
public class FootTrafficZoneParams {

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

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "hourly_curve_sat", columnDefinition = "double precision[24]", nullable = false)
    private Double[] hourlyCurveSat;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "hourly_curve_sun", columnDefinition = "double precision[24]", nullable = false)
    private Double[] hourlyCurveSun;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "seasonal_scalers", columnDefinition = "double precision[12]", nullable = false)
    private Double[] seasonalScalers;

    @Column(name = "day_scaler_sat", nullable = false)
    private Double dayScalerSat;

    @Column(name = "day_scaler_sun", nullable = false)
    private Double dayScalerSun;

    @Column(name = "noise_sigma", nullable = false)
    private Double noiseSigma;

    @Column(name = "updated_at", nullable = false)
    private Timestamp updatedAt;
}
