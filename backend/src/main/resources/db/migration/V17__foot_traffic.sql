-- Foot traffic simulation: global archetype params + per-cell precomputed profile

CREATE TABLE foot_traffic_zone_params (
    archetype VARCHAR(32) PRIMARY KEY,
    base_daily_min INTEGER NOT NULL,
    base_daily_max INTEGER NOT NULL,
    poi_density_cap DOUBLE PRECISION NOT NULL DEFAULT 50,
    pop_density_cap DOUBLE PRECISION NOT NULL DEFAULT 20000,
    income_weight DOUBLE PRECISION NOT NULL DEFAULT 0.1,
    hourly_curve_wd DOUBLE PRECISION[24] NOT NULL,
    hourly_curve_sat DOUBLE PRECISION[24] NOT NULL,
    hourly_curve_sun DOUBLE PRECISION[24] NOT NULL,
    seasonal_scalers DOUBLE PRECISION[12] NOT NULL,
    day_scaler_sat DOUBLE PRECISION NOT NULL DEFAULT 0.85,
    day_scaler_sun DOUBLE PRECISION NOT NULL DEFAULT 0.70,
    noise_sigma DOUBLE PRECISION NOT NULL DEFAULT 0.05,
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE foot_traffic_cell_profile (
    h3_index VARCHAR(15) PRIMARY KEY REFERENCES h3_hexagon(h3_index) ON DELETE CASCADE,
    scenario_id INTEGER NOT NULL DEFAULT 0,
    archetype VARCHAR(32) NOT NULL REFERENCES foot_traffic_zone_params(archetype),
    archetype_confidence DOUBLE PRECISION NOT NULL DEFAULT 0,
    baseline_daily INTEGER NOT NULL,
    peak_hourly INTEGER NOT NULL,
    driver_poi_count INTEGER NOT NULL DEFAULT 0,
    competitor_poi_count INTEGER NOT NULL DEFAULT 0,
    transit_poi_count INTEGER NOT NULL DEFAULT 0,
    pop_density DOUBLE PRECISION NOT NULL DEFAULT 0,
    avg_income DOUBLE PRECISION NOT NULL DEFAULT 0,
    noise_seed BIGINT NOT NULL,
    computed_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ft_cell_profile_archetype ON foot_traffic_cell_profile(archetype);
CREATE INDEX idx_ft_cell_profile_scenario ON foot_traffic_cell_profile(scenario_id);

-- Default seasonal scalers (Northern Hemisphere, Jan..Dec)
-- Shared across archetypes for V1 seed
-- Jan=0.85, Feb=0.87, Mar=0.92, Apr=0.98, May=1.05, Jun=1.08, Jul=1.10, Aug=1.05, Sep=1.02, Oct=0.97, Nov=0.90, Dec=0.88
INSERT INTO foot_traffic_zone_params (
    archetype, base_daily_min, base_daily_max, poi_density_cap, pop_density_cap, income_weight,
    hourly_curve_wd, hourly_curve_sat, hourly_curve_sun, seasonal_scalers, day_scaler_sat, day_scaler_sun, noise_sigma
) VALUES
(
    'CBD', 8000, 40000, 50, 20000, 0.1,
    ARRAY[0.1,0.05,0.05,0.05,0.1,0.3,0.8,1.8,2.2,1.9,1.7,1.8,2.0,1.7,1.6,1.7,1.9,2.1,1.5,1.0,0.7,0.5,0.3,0.15]::double precision[],
    ARRAY[0.08,0.05,0.05,0.05,0.08,0.2,0.5,1.0,1.4,1.5,1.6,1.7,1.8,1.6,1.5,1.6,1.8,1.9,1.5,1.0,0.7,0.5,0.3,0.12]::double precision[],
    ARRAY[0.06,0.04,0.04,0.04,0.06,0.15,0.4,0.8,1.2,1.3,1.4,1.5,1.6,1.4,1.3,1.4,1.5,1.6,1.2,0.9,0.6,0.4,0.25,0.1]::double precision[],
    ARRAY[0.85,0.87,0.92,0.98,1.05,1.08,1.10,1.05,1.02,0.97,0.90,0.88]::double precision[],
    0.85, 0.70, 0.05
),
(
    'OFFICE', 3000, 15000, 50, 20000, 0.1,
    ARRAY[0.05,0.03,0.03,0.03,0.05,0.2,0.9,2.0,2.3,2.0,1.6,1.5,1.6,1.5,1.4,1.5,1.8,2.0,1.2,0.6,0.4,0.25,0.15,0.08]::double precision[],
    ARRAY[0.04,0.02,0.02,0.02,0.04,0.15,0.6,1.4,1.8,1.5,1.2,1.1,1.2,1.1,1.0,1.1,1.4,1.5,1.0,0.5,0.35,0.2,0.12,0.06]::double precision[],
    ARRAY[0.03,0.02,0.02,0.02,0.03,0.1,0.4,1.0,1.4,1.2,1.0,0.9,1.0,0.9,0.85,0.9,1.1,1.2,0.8,0.4,0.3,0.15,0.08,0.04]::double precision[],
    ARRAY[0.85,0.87,0.92,0.98,1.05,1.08,1.10,1.05,1.02,0.97,0.90,0.88]::double precision[],
    0.85, 0.70, 0.05
),
(
    'RETAIL', 2500, 20000, 50, 20000, 0.1,
    ARRAY[0.1,0.05,0.05,0.05,0.1,0.2,0.4,0.7,1.0,1.2,1.4,1.6,1.9,1.7,1.5,1.6,1.8,2.0,1.9,1.6,1.2,0.8,0.5,0.2]::double precision[],
    ARRAY[0.08,0.04,0.04,0.04,0.08,0.15,0.35,0.8,1.2,1.5,1.7,1.9,2.0,1.9,1.7,1.8,2.0,2.1,2.0,1.7,1.3,0.9,0.6,0.25]::double precision[],
    ARRAY[0.07,0.04,0.04,0.04,0.07,0.12,0.3,0.7,1.1,1.4,1.6,1.8,1.9,1.8,1.6,1.7,1.9,2.0,1.9,1.5,1.1,0.7,0.45,0.2]::double precision[],
    ARRAY[0.85,0.87,0.92,0.98,1.05,1.08,1.10,1.05,1.02,0.97,0.90,0.88]::double precision[],
    0.85, 0.70, 0.05
),
(
    'TRANSIT_HUB', 5000, 50000, 50, 20000, 0.1,
    ARRAY[0.08,0.05,0.05,0.05,0.1,0.4,1.2,2.5,2.8,2.0,1.4,1.3,1.4,1.3,1.2,1.4,1.8,2.6,2.4,1.2,0.8,0.5,0.3,0.12]::double precision[],
    ARRAY[0.06,0.04,0.04,0.04,0.08,0.3,0.9,2.0,2.2,1.6,1.2,1.1,1.2,1.1,1.0,1.2,1.6,2.2,2.0,1.0,0.7,0.45,0.25,0.1]::double precision[],
    ARRAY[0.05,0.03,0.03,0.03,0.06,0.25,0.7,1.6,1.8,1.3,1.0,0.9,1.0,0.9,0.85,1.0,1.4,1.8,1.6,0.8,0.55,0.35,0.2,0.08]::double precision[],
    ARRAY[0.85,0.87,0.92,0.98,1.05,1.08,1.10,1.05,1.02,0.97,0.90,0.88]::double precision[],
    0.85, 0.70, 0.05
),
(
    'CAMPUS', 1500, 10000, 50, 20000, 0.1,
    ARRAY[0.05,0.03,0.03,0.03,0.05,0.15,0.6,2.0,2.2,1.8,1.2,1.8,2.0,1.2,1.0,1.2,1.5,1.8,1.4,0.8,0.5,0.3,0.15,0.06]::double precision[],
    ARRAY[0.04,0.02,0.02,0.02,0.04,0.12,0.4,1.4,1.6,1.3,0.9,1.4,1.6,1.0,0.85,1.0,1.2,1.4,1.1,0.6,0.4,0.25,0.12,0.05]::double precision[],
    ARRAY[0.03,0.02,0.02,0.02,0.03,0.08,0.25,0.9,1.0,0.8,0.5,0.8,1.0,0.6,0.5,0.6,0.8,1.0,0.8,0.45,0.3,0.18,0.08,0.03]::double precision[],
    ARRAY[0.85,0.87,0.92,0.98,1.05,1.08,1.10,1.05,1.02,0.97,0.90,0.88]::double precision[],
    0.85, 0.70, 0.05
),
(
    'RESIDENTIAL', 400, 3000, 50, 20000, 0.1,
    ARRAY[0.2,0.1,0.1,0.1,0.2,0.5,1.0,1.5,1.3,0.9,0.8,0.9,1.1,0.8,0.8,0.9,1.1,1.4,1.5,1.3,1.0,0.8,0.5,0.3]::double precision[],
    ARRAY[0.18,0.09,0.09,0.09,0.18,0.45,0.9,1.3,1.1,0.8,0.7,0.8,1.0,0.75,0.75,0.85,1.0,1.2,1.3,1.1,0.9,0.7,0.45,0.25]::double precision[],
    ARRAY[0.15,0.08,0.08,0.08,0.15,0.4,0.8,1.1,1.0,0.7,0.6,0.7,0.9,0.65,0.65,0.75,0.9,1.1,1.2,1.0,0.85,0.65,0.4,0.22]::double precision[],
    ARRAY[0.85,0.87,0.92,0.98,1.05,1.08,1.10,1.05,1.02,0.97,0.90,0.88]::double precision[],
    0.85, 0.70, 0.05
),
(
    'PARK', 200, 4000, 50, 20000, 0.1,
    ARRAY[0.15,0.08,0.08,0.08,0.12,0.25,0.5,0.9,1.1,1.2,1.3,1.4,1.5,1.3,1.2,1.3,1.2,1.0,0.7,0.5,0.35,0.25,0.18,0.12]::double precision[],
    ARRAY[0.06,0.04,0.04,0.04,0.08,0.2,0.6,1.2,1.5,1.6,1.7,1.8,1.9,1.7,1.6,1.7,1.6,1.3,0.9,0.6,0.45,0.35,0.25,0.15]::double precision[],
    ARRAY[0.05,0.03,0.03,0.03,0.06,0.18,0.55,1.1,1.4,1.5,1.6,1.7,1.8,1.6,1.5,1.6,1.5,1.2,0.85,0.55,0.4,0.3,0.2,0.12]::double precision[],
    ARRAY[0.85,0.87,0.92,0.98,1.05,1.08,1.10,1.05,1.02,0.97,0.90,0.88]::double precision[],
    0.85, 0.70, 0.05
),
(
    'RURAL', 10, 200, 50, 20000, 0.1,
    ARRAY[0.25,0.15,0.12,0.12,0.15,0.35,0.8,1.2,1.1,1.0,1.0,1.1,1.2,1.0,0.95,1.0,1.1,1.0,0.85,0.7,0.55,0.4,0.3,0.22]::double precision[],
    ARRAY[0.2,0.12,0.1,0.1,0.12,0.3,0.7,1.0,0.95,0.9,0.9,1.0,1.1,0.95,0.9,0.95,1.0,0.95,0.8,0.65,0.5,0.35,0.25,0.18]::double precision[],
    ARRAY[0.18,0.1,0.08,0.08,0.1,0.25,0.6,0.9,0.85,0.8,0.8,0.9,1.0,0.85,0.8,0.85,0.9,0.85,0.7,0.55,0.45,0.3,0.22,0.15]::double precision[],
    ARRAY[0.85,0.87,0.92,0.98,1.05,1.08,1.10,1.05,1.02,0.97,0.90,0.88]::double precision[],
    0.85, 0.70, 0.05
);
