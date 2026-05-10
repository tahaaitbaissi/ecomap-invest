package com.example.backend.demographics;

import com.uber.h3core.H3Core;
import com.uber.h3core.util.LatLng;
import java.util.Random;

/**
 * Deterministic synthetic population density and income when no demographics row exists
 * (docker demo / Morocco grid). Matches the original {@code DemographicsSeeder} RNG so seeded DB
 * rows and runtime fallback stay identical for the same H3 index and salt.
 *
 * <p>The RNG is intentionally deterministic and non-cryptographic (visualization / demo data only).
 */
public final class DemographicsSynth {

    public record DensityIncome(double populationDensity, double avgIncome) {}

    private DemographicsSynth() {}

    public static DensityIncome forHex(
            String h3Index, H3Core h3, long salt, double centreLat, double centreLng) {
        Random rnd =
                stableRnd(h3Index != null ? h3Index.hashCode() : 0, salt);

        double centre01 = cityCentre01(h3Index, h3, centreLat, centreLng, rnd);
        int low = 1000;
        int high = 18_000;
        double t = rnd.nextDouble();
        double population = Math.clamp((low + t * (high - low)) * (0.5 + 0.5 * centre01), low, high);

        double incomeLow = 4000;
        double incomeHigh = 30_000;
        double income =
                incomeLow
                        + rnd.nextDouble()
                                * (incomeHigh - incomeLow)
                                * (0.85 + 0.15 * centre01);
        income = Math.clamp(income, incomeLow, incomeHigh);

        return new DensityIncome(population, income);
    }

    /**
     * Seeded {@link Random} for reproducible synthetic values — not used for secrets or crypto.
     */
    @SuppressWarnings("java:S2245")
    static Random stableRnd(int hIndexHashCode, long salt) {
        return new Random(mix64(hIndexHashCode, salt)); // NOSONAR — deterministic demo synthesis
    }

    private static long mix64(int h, long s) {
        long x = h ^ s;
        x = (x ^ (x >>> 33)) * 0xff51afd7ed558ccdL;
        x = (x ^ (x >>> 33)) * 0xc4ceb9fe1a85ec53L;
        return x ^ (x >>> 33);
    }

    private static double cityCentre01(
            String h3Index, H3Core h3, double centreLat, double centreLng, Random rnd) {
        try {
            long cell = h3.stringToH3(h3Index);
            LatLng c = h3.cellToLatLng(cell);
            double dLat = c.lat - centreLat;
            double dLng = c.lng - centreLng;
            double d2 = dLat * dLat + dLng * dLng;
            return 1.0 - Math.clamp(d2 / 0.0225, 0, 1);
        } catch (Exception e) {
            return 0.3 + 0.7 * rnd.nextDouble();
        }
    }
}
