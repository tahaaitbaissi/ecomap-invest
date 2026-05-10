package com.ecomap.foottraffic.simulation;

import java.util.Arrays;
import java.util.Objects;

/** Immutable zone parameters for {@link FootTrafficBaselineCalculator} (DB-agnostic). */
public record FootTrafficZoneParamsSnapshot(
        String archetype,
        int baseDailyMin,
        int baseDailyMax,
        double poiDensityCap,
        double popDensityCap,
        double incomeWeight,
        Double[] hourlyCurveWd,
        double noiseSigma) {

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        FootTrafficZoneParamsSnapshot that = (FootTrafficZoneParamsSnapshot) o;
        return baseDailyMin == that.baseDailyMin
                && baseDailyMax == that.baseDailyMax
                && Double.compare(poiDensityCap, that.poiDensityCap) == 0
                && Double.compare(popDensityCap, that.popDensityCap) == 0
                && Double.compare(incomeWeight, that.incomeWeight) == 0
                && Double.compare(noiseSigma, that.noiseSigma) == 0
                && Objects.equals(archetype, that.archetype)
                && Arrays.equals(hourlyCurveWd, that.hourlyCurveWd);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                archetype,
                baseDailyMin,
                baseDailyMax,
                poiDensityCap,
                popDensityCap,
                incomeWeight,
                Arrays.hashCode(hourlyCurveWd),
                noiseSigma);
    }

    @Override
    public String toString() {
        return "FootTrafficZoneParamsSnapshot[archetype="
                + archetype
                + ", baseDailyMin="
                + baseDailyMin
                + ", baseDailyMax="
                + baseDailyMax
                + ", poiDensityCap="
                + poiDensityCap
                + ", popDensityCap="
                + popDensityCap
                + ", incomeWeight="
                + incomeWeight
                + ", hourlyCurveWd="
                + Arrays.toString(hourlyCurveWd)
                + ", noiseSigma="
                + noiseSigma
                + "]";
    }
}
