package com.ecomap.foottraffic.simulation;

import java.util.function.Function;

/** Single entry for deterministic foot-traffic cell simulation (used in-process and by SOAP server). */
public final class FootTrafficSimulationEngine {

    private static final String FALLBACK_ARCHETYPE = "RESIDENTIAL";

    public record CellSimulationResult(
            String archetype,
            double archetypeConfidence,
            int baselineDaily,
            int peakHourly,
            long noiseSeed,
            int driverPoiCount,
            int competitorPoiCount,
            int transitPoiCount) {}

    private FootTrafficSimulationEngine() {}

    /**
     * @param zoneParamsResolver maps archetype string to params; must return non-null for FALLBACK_ARCHETYPE
     */
    public static CellSimulationResult simulate(
            PoiTagCounts counts,
            double populationDensity,
            double avgIncome,
            String h3Index,
            long jitterSalt,
            Function<String, FootTrafficZoneParamsSnapshot> zoneParamsResolver) {
        FootTrafficZoneClassifier.ArchetypeResult arch =
                FootTrafficZoneClassifier.classify(counts, populationDensity);
        FootTrafficZoneParamsSnapshot params = zoneParamsResolver.apply(arch.archetype());
        if (params == null) {
            params = zoneParamsResolver.apply(FALLBACK_ARCHETYPE);
        }
        FootTrafficBaselineCalculator.BaselineResult baseline =
                FootTrafficBaselineCalculator.calc(
                        counts, params, populationDensity, avgIncome, h3Index, jitterSalt);
        int driverTotal = (int) Math.min(Integer.MAX_VALUE, counts.total());
        int comp = (int) Math.min(Integer.MAX_VALUE, counts.competitorApprox());
        int transit = (int) Math.min(Integer.MAX_VALUE, counts.transitPoiCount() + counts.railwayStationCount());
        return new CellSimulationResult(
                arch.archetype(),
                arch.confidence(),
                baseline.baselineDaily(),
                baseline.peakHourly(),
                baseline.noiseSeed(),
                driverTotal,
                comp,
                transit);
    }
}
