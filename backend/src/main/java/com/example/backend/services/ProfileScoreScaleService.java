package com.example.backend.services;

import com.example.backend.controllers.dto.ProfileScoreNormDebugResponse;
import com.example.backend.controllers.dto.ProfileScoreNormDebugResponse.HistogramBucket;
import com.example.backend.repositories.H3HexagonRepository;
import com.example.backend.scoring.HexScoringConfig;
import com.example.backend.scoring.HexagonRawScoringSupport;
import com.example.backend.scoring.RawScoreRefBounds;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

/**
 * Caches raw score stretch bounds over the seeded {@code h3_hexagon} grid so heatmap normalization
 * does not depend on map pan/zoom bbox. By default uses robust percentile endpoints (p5–p95) instead
 * of global min/max to limit outlier-driven scale distortion.
 */
@Slf4j
@Service
public class ProfileScoreScaleService {

    /** Bumped when cached shape/semantics change — old keys are ignored. */
    private static final String REDIS_KEY_PREFIX = "scoreNorm:v2:";

    private static final int HISTOGRAM_BINS = 10;

    private final HexagonRawScoringSupport rawScoringSupport;
    private final H3HexagonRepository h3HexagonRepository;
    private final ObjectMapper objectMapper;

    @Nullable
    private final StringRedisTemplate stringRedisTemplate;

    @Value("${app.hexagon.normalization.max-reference-cells:25000}")
    private int maxReferenceCells;

    @Value("${app.hexagon.normalization.cache-ttl-seconds:86400}")
    private int normCacheTtlSeconds;

    @Value("${app.hexagon.normalization.percentile-low:5}")
    private double stretchPercentileLow;

    @Value("${app.hexagon.normalization.percentile-high:95}")
    private double stretchPercentileHigh;

    @Value("${app.hexagon.normalization.min-samples-for-percentiles:20}")
    private int minSamplesForPercentiles;

    @Autowired
    public ProfileScoreScaleService(
            HexagonRawScoringSupport rawScoringSupport,
            H3HexagonRepository h3HexagonRepository,
            ObjectMapper objectMapper,
            @Nullable StringRedisTemplate stringRedisTemplate) {
        this.rawScoringSupport = rawScoringSupport;
        this.h3HexagonRepository = h3HexagonRepository;
        this.objectMapper = objectMapper;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public void invalidate(UUID profileId) {
        if (stringRedisTemplate == null || normCacheTtlSeconds <= 0) {
            return;
        }
        try {
            stringRedisTemplate.delete(redisKey(profileId));
        } catch (DataAccessException e) {
            log.warn("Redis delete scoreNorm failed for {}: {}", profileId, e.getMessage());
        }
    }

    /**
     * Resolves stretch low/high raw values for normalization. Uses Redis when available; otherwise
     * recomputes over up to {@code maxReferenceCells} grid cells.
     */
    public RawScoreRefBounds resolveRefBounds(UUID profileId, HexScoringConfig cfg) {
        if (normCacheTtlSeconds > 0 && stringRedisTemplate != null) {
            try {
                String json = stringRedisTemplate.opsForValue().get(redisKey(profileId));
                if (json != null && !json.isBlank()) {
                    BoundsDto dto = objectMapper.readValue(json, BoundsDto.class);
                    if (dto != null && Double.isFinite(dto.min()) && Double.isFinite(dto.max())) {
                        return new RawScoreRefBounds(dto.min(), dto.max());
                    }
                }
            } catch (JsonProcessingException e) {
                log.warn("Malformed scoreNorm cache for {} — recomputing", profileId);
            } catch (DataAccessException e) {
                log.warn("Redis GET scoreNorm failed for {} — recomputing", profileId);
            }
        }
        RawScoreRefBounds computed = computeRefBounds(profileId, cfg);
        cache(profileId, computed);
        return computed;
    }

    /**
     * Full raw distribution and effective stretch bounds for debugging (always recomputed, not read
     * from Redis).
     */
    public ProfileScoreNormDebugResponse computeNormDebug(UUID profileId, HexScoringConfig cfg) {
        Objects.requireNonNull(profileId, "profileId");
        double[] sorted = collectFiniteSortedRaws(cfg);
        if (sorted.length == 0) {
            return emptyDebug(profileId);
        }
        StretchResult stretch = stretchFromSorted(sorted);
        List<HistogramBucket> hist =
                buildHistogram(sorted, stretch.globalMin(), stretch.globalMax());
        return new ProfileScoreNormDebugResponse(
                profileId,
                sorted.length,
                maxReferenceCells,
                stretch.globalMin(),
                stretch.globalMax(),
                stretch.pLow(),
                stretch.pMid(),
                stretch.pHigh(),
                stretch.bounds().min(),
                stretch.bounds().max(),
                stretch.mode(),
                hist);
    }

    private ProfileScoreNormDebugResponse emptyDebug(UUID profileId) {
        return new ProfileScoreNormDebugResponse(
                profileId,
                0,
                maxReferenceCells,
                Double.NaN,
                Double.NaN,
                Double.NaN,
                Double.NaN,
                Double.NaN,
                0.0,
                0.0,
                ProfileScoreNormDebugResponse.StretchMode.DEGENERATE,
                List.of());
    }

    private void cache(UUID profileId, RawScoreRefBounds b) {
        if (stringRedisTemplate == null || normCacheTtlSeconds <= 0) {
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(new BoundsDto(b.min(), b.max()));
            stringRedisTemplate
                    .opsForValue()
                    .set(redisKey(profileId), json, java.time.Duration.ofSeconds(normCacheTtlSeconds));
        } catch (Exception e) {
            log.warn("Redis SET scoreNorm failed for {}: {}", profileId, e.getMessage());
        }
    }

    private RawScoreRefBounds computeRefBounds(UUID profileId, HexScoringConfig cfg) {
        double[] sorted = collectFiniteSortedRaws(cfg);
        if (sorted.length == 0) {
            log.debug("scoreNorm compute: no finite raws for profile {}", profileId);
            return RawScoreRefBounds.degenerate(0.0);
        }
        StretchResult stretch = stretchFromSorted(sorted);
        return stretch.bounds();
    }

    private double[] collectFiniteSortedRaws(HexScoringConfig cfg) {
        var indices = h3HexagonRepository.findAllH3IndicesLimited(maxReferenceCells);
        double[] buf = new double[indices.size()];
        int n = 0;
        for (String h3 : indices) {
            double r = rawScoringSupport.computeRaw(h3, cfg);
            if (Double.isFinite(r)) {
                buf[n++] = r;
            }
        }
        double[] raws = Arrays.copyOf(buf, n);
        Arrays.sort(raws);
        return raws;
    }

    private StretchResult stretchFromSorted(double[] sorted) {
        int n = sorted.length;
        double gMin = sorted[0];
        double gMax = sorted[n - 1];
        double pLo = percentileLinear(sorted, stretchPercentileLow);
        double pMid = percentileLinear(sorted, 50.0);
        double pHi = percentileLinear(sorted, stretchPercentileHigh);

        boolean usePercentile =
                n >= minSamplesForPercentiles && Double.isFinite(pLo) && Double.isFinite(pHi) && pHi > pLo;

        if (!Double.isFinite(gMin) || !Double.isFinite(gMax)) {
            return new StretchResult(
                    RawScoreRefBounds.degenerate(0.0),
                    ProfileScoreNormDebugResponse.StretchMode.DEGENERATE,
                    gMin,
                    gMax,
                    pLo,
                    pMid,
                    pHi);
        }
        if (gMin == gMax) {
            return new StretchResult(
                    RawScoreRefBounds.degenerate(gMin),
                    ProfileScoreNormDebugResponse.StretchMode.DEGENERATE,
                    gMin,
                    gMax,
                    pLo,
                    pMid,
                    pHi);
        }

        if (usePercentile) {
            return new StretchResult(
                    new RawScoreRefBounds(pLo, pHi),
                    ProfileScoreNormDebugResponse.StretchMode.PERCENTILE,
                    gMin,
                    gMax,
                    pLo,
                    pMid,
                    pHi);
        }

        return new StretchResult(
                new RawScoreRefBounds(gMin, gMax),
                ProfileScoreNormDebugResponse.StretchMode.MIN_MAX,
                gMin,
                gMax,
                pLo,
                pMid,
                pHi);
    }

    /** Linear interpolation percentile on a sorted array; {@code p} in [0, 100]. */
    static double percentileLinear(double[] sortedAscending, double p) {
        if (sortedAscending.length == 0) {
            return Double.NaN;
        }
        if (sortedAscending.length == 1) {
            return sortedAscending[0];
        }
        double clamped = Math.max(0.0, Math.min(100.0, p));
        double pos = (sortedAscending.length - 1) * (clamped / 100.0);
        int lo = (int) Math.floor(pos);
        int hi = (int) Math.ceil(pos);
        if (lo == hi) {
            return sortedAscending[lo];
        }
        return sortedAscending[lo]
                + (sortedAscending[hi] - sortedAscending[lo]) * (pos - lo);
    }

    private static List<HistogramBucket> buildHistogram(double[] sortedAscending, double gMin, double gMax) {
        if (sortedAscending.length == 0) {
            return List.of();
        }
        if (!(gMax > gMin)) {
            return List.of(new HistogramBucket(gMin, gMax, sortedAscending.length));
        }
        int[] counts = new int[HISTOGRAM_BINS];
        double span = gMax - gMin;
        for (double v : sortedAscending) {
            int i = (int) Math.floor((v - gMin) / span * HISTOGRAM_BINS);
            if (i < 0) {
                i = 0;
            } else if (i >= HISTOGRAM_BINS) {
                i = HISTOGRAM_BINS - 1;
            }
            counts[i]++;
        }
        List<HistogramBucket> out = new ArrayList<>(HISTOGRAM_BINS);
        for (int i = 0; i < HISTOGRAM_BINS; i++) {
            double lo = gMin + span * i / HISTOGRAM_BINS;
            double hi = (i == HISTOGRAM_BINS - 1) ? gMax : gMin + span * (i + 1) / HISTOGRAM_BINS;
            out.add(new HistogramBucket(lo, hi, counts[i]));
        }
        return out;
    }

    private static String redisKey(UUID profileId) {
        return REDIS_KEY_PREFIX + profileId;
    }

    /** Display score for heatmap cells (baseline raw only). */
    public static Double toHeatmapDisplayScore(double raw, RawScoreRefBounds ref, int viewportCellCount) {
        if (!Double.isFinite(raw)) {
            return null;
        }
        if (viewportCellCount >= 2 && ref.flat()) {
            return null;
        }
        if (ref.flat()) {
            return 50.0;
        }
        double span = ref.max() - ref.min();
        double t = (raw - ref.min()) / span;
        double s = t * 100.0;
        return Math.max(0.0, Math.min(100.0, s));
    }

    /** Display score after applying simulation delta to baseline raw. */
    public static double toSimAdjustedDisplay(double baselineRawPlusDelta, RawScoreRefBounds ref) {
        if (!Double.isFinite(baselineRawPlusDelta)) {
            return 50.0;
        }
        if (ref.flat()) {
            double anchor = ref.min();
            return Math.max(0.0, Math.min(100.0, 50.0 + (baselineRawPlusDelta - anchor)));
        }
        double span = ref.max() - ref.min();
        double s = 100.0 * (baselineRawPlusDelta - ref.min()) / span;
        return Math.max(0.0, Math.min(100.0, s));
    }

    private record StretchResult(
            RawScoreRefBounds bounds,
            ProfileScoreNormDebugResponse.StretchMode mode,
            double globalMin,
            double globalMax,
            double pLow,
            double pMid,
            double pHigh) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private record BoundsDto(double min, double max) {}
}
