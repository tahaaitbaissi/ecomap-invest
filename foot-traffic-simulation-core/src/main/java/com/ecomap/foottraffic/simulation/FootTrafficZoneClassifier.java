package com.ecomap.foottraffic.simulation;

import java.util.Locale;

/** Rule-based zone archetype from OSM POI mix (no ML). */
public final class FootTrafficZoneClassifier {

    public record ArchetypeResult(String archetype, double confidence) {}

    private FootTrafficZoneClassifier() {}

    public static ArchetypeResult classify(PoiTagCounts c, double popDensity) {
        long total = Math.max(1, c.total());

        if (c.total() < 3) {
            // Sparse OSM tagging but obvious workplace or strong population → not "empty countryside"
            if ((c.workplaceFootprintCount() >= 1 && popDensity > 800) || popDensity > 2500) {
                double conf =
                        Math.min(
                                1.0,
                                Math.max(
                                        (double) c.workplaceFootprintCount() / 3.0,
                                        popDensity / 15_000.0));
                return new ArchetypeResult("RESIDENTIAL", conf);
            }
            return new ArchetypeResult("RURAL", (double) c.total() / 3.0);
        }

        if (c.transitPoiCount() >= 3 || c.railwayStationCount() >= 1) {
            long dom = Math.max(c.transitPoiCount(), c.railwayStationCount() * 3L);
            double conf = Math.min(1.0, (double) dom / Math.max(3, total));
            return new ArchetypeResult("TRANSIT_HUB", conf);
        }

        long cbdDrivers = c.cbdOfficeBankHotel();
        if (cbdDrivers >= 8 && popDensity > 5000) {
            double conf = Math.min(1.0, (double) cbdDrivers / Math.max(8, total));
            return new ArchetypeResult("CBD", conf);
        }

        if (c.officeCount() >= 5) {
            double conf = Math.min(1.0, (double) c.officeCount() / Math.max(5, total));
            return new ArchetypeResult("OFFICE", conf);
        }

        if (c.campusEducationCount() >= 4) {
            double conf = Math.min(1.0, (double) c.campusEducationCount() / Math.max(4, total));
            return new ArchetypeResult("CAMPUS", conf);
        }

        if (c.retailFoodCount() >= 10) {
            double conf = Math.min(1.0, (double) c.retailFoodCount() / Math.max(10, total));
            return new ArchetypeResult("RETAIL", conf);
        }

        long nonPark = c.total() - c.parkNature();
        if (c.parkNature() >= 2 && nonPark < 5) {
            double conf = Math.min(1.0, (double) c.parkNature() / Math.max(2, total));
            return new ArchetypeResult("PARK", conf);
        }

        if (popDensity > 1000 || c.residentialCount() >= 3) {
            double dom = Math.max(popDensity > 1000 ? 1.0 : 0, c.residentialCount());
            double conf = Math.min(1.0, dom / Math.max(1, total));
            return new ArchetypeResult("RESIDENTIAL", conf);
        }

        return new ArchetypeResult("RURAL", Math.min(1.0, (double) c.total() / 10.0));
    }

    public static String normalizeArchetype(String raw) {
        if (raw == null || raw.isBlank()) {
            return "RESIDENTIAL";
        }
        return raw.trim().toUpperCase(Locale.ROOT);
    }
}
