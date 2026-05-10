package com.ecomap.foottraffic.simulation;

/** Deterministic daily baseline and peak-hourly estimate from POI mix + demographics. */
public final class FootTrafficBaselineCalculator {

    public record BaselineResult(int baselineDaily, int peakHourly, long noiseSeed) {}

    private static final double INCOME_CAP = 150_000.0;

    private FootTrafficBaselineCalculator() {}

    public static BaselineResult calc(
            PoiTagCounts counts,
            FootTrafficZoneParamsSnapshot params,
            double popDensity,
            double avgIncome,
            String h3Index,
            long globalSalt) {
        double poiCap = Math.max(1.0, params.poiDensityCap());
        double popCap = Math.max(1.0, params.popDensityCap());
        double incW = Math.max(0.0, params.incomeWeight());

        double poiScore = Math.min((double) counts.total() / poiCap, 1.0);
        double popScore = Math.min(Math.max(0.0, popDensity) / popCap, 1.0);
        double incScore = Math.min(Math.max(0.0, avgIncome) / INCOME_CAP, 1.0) * incW;

        double combined = 0.55 * poiScore + 0.35 * popScore + 0.10 * incScore;
        combined = Math.min(1.0, Math.max(0.0, combined));

        int baseMin = params.baseDailyMin();
        int baseMax = params.baseDailyMax();
        double rawBaseline = baseMin + combined * (baseMax - baseMin);

        long noiseSeed = FootTrafficMath.murmurHash64(h3Index, globalSalt);
        double jitter = FootTrafficMath.jitterMultiplier(noiseSeed, params.noiseSigma());
        int baselineDaily = (int) Math.round(rawBaseline * jitter);
        baselineDaily = Math.max(baseMin, Math.min(baseMax * 2, baselineDaily));

        double maxWd = maxOf(params.hourlyCurveWd());
        double peakShare = maxWd / 24.0;
        int peakHourly = (int) Math.round(baselineDaily * peakShare);

        return new BaselineResult(baselineDaily, Math.max(0, peakHourly), noiseSeed);
    }

    private static double maxOf(Double[] arr) {
        if (arr == null || arr.length == 0) {
            return 1.0;
        }
        double m = arr[0] != null ? arr[0] : 0.0;
        for (int i = 1; i < arr.length; i++) {
            double v = arr[i] != null ? arr[i] : 0.0;
            if (v > m) m = v;
        }
        return m;
    }
}
