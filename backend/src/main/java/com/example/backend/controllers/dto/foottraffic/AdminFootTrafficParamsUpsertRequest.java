package com.example.backend.controllers.dto.foottraffic;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record AdminFootTrafficParamsUpsertRequest(
        @NotNull Integer baseDailyMin,
        @NotNull Integer baseDailyMax,
        @NotNull Double poiDensityCap,
        @NotNull Double popDensityCap,
        @NotNull Double incomeWeight,
        @NotNull @Size(min = 24, max = 24) List<Double> hourlyCurveWd,
        @NotNull @Size(min = 24, max = 24) List<Double> hourlyCurveSat,
        @NotNull @Size(min = 24, max = 24) List<Double> hourlyCurveSun,
        @NotNull @Size(min = 12, max = 12) List<Double> seasonalScalers,
        @NotNull Double dayScalerSat,
        @NotNull Double dayScalerSun,
        @NotNull Double noiseSigma) {}
