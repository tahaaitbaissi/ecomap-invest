package com.example.backend.foottraffic.simulation;

import com.example.backend.foottraffic.entities.FootTrafficZoneParams;
import java.util.Arrays;

public final class FootTrafficHourlyCurveGenerator {

    private FootTrafficHourlyCurveGenerator() {}

    public static double[] generate(
            FootTrafficZoneParams params, DayType dayType, int monthIndex, int baselineDaily) {
        Double[] multipliers = curveFor(params, dayType);
        int m = Math.max(0, Math.min(11, monthIndex));
        Double[] seasonal = params.getSeasonalScalers();
        double seasonalScaler =
                seasonal != null && m < seasonal.length && seasonal[m] != null
                        ? seasonal[m]
                        : 1.0;

        double dayScaler =
                switch (dayType) {
                    case WD -> 1.0;
                    case SAT -> params.getDayScalerSat() != null ? params.getDayScalerSat() : 0.85;
                    case SUN -> params.getDayScalerSun() != null ? params.getDayScalerSun() : 0.70;
                };

        double budget = baselineDaily * seasonalScaler * dayScaler;
        double[] hourly = new double[24];
        double sumMult = 0.0;
        for (int h = 0; h < 24; h++) {
            double mm = multipliers != null && h < multipliers.length && multipliers[h] != null
                    ? multipliers[h]
                    : 1.0;
            sumMult += mm;
        }
        if (sumMult <= 0) {
            Arrays.fill(hourly, budget / 24.0);
            return hourly;
        }
        for (int h = 0; h < 24; h++) {
            double mm = multipliers != null && h < multipliers.length && multipliers[h] != null
                    ? multipliers[h]
                    : 1.0;
            hourly[h] = budget * (mm / sumMult);
        }
        return hourly;
    }

    private static Double[] curveFor(FootTrafficZoneParams params, DayType dayType) {
        return switch (dayType) {
            case WD -> params.getHourlyCurveWd();
            case SAT -> params.getHourlyCurveSat();
            case SUN -> params.getHourlyCurveSun();
        };
    }

    public static int peakHourIndex(double[] hourly) {
        if (hourly == null || hourly.length == 0) {
            return 0;
        }
        int idx = 0;
        double m = hourly[0];
        for (int i = 1; i < hourly.length; i++) {
            if (hourly[i] > m) {
                m = hourly[i];
                idx = i;
            }
        }
        return idx;
    }

    public static double sum(double[] hourly) {
        double s = 0;
        if (hourly == null) {
            return 0;
        }
        for (double v : hourly) {
            s += v;
        }
        return s;
    }
}
