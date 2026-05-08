package com.example.backend.foottraffic.services;

import com.example.backend.entities.Demographics;
import com.example.backend.foottraffic.config.FootTrafficProperties;
import com.example.backend.foottraffic.repositories.FootTrafficCellProfileRepository;
import com.example.backend.foottraffic.repositories.FootTrafficZoneParamsRepository;
import com.example.backend.foottraffic.simulation.FootTrafficBaselineCalculator;
import com.example.backend.foottraffic.simulation.FootTrafficBaselineCalculator.BaselineResult;
import com.example.backend.foottraffic.simulation.FootTrafficZoneClassifier;
import com.example.backend.foottraffic.simulation.FootTrafficZoneClassifier.ArchetypeResult;
import com.example.backend.foottraffic.simulation.PoiFootTrafficTagAggregator;
import com.example.backend.foottraffic.simulation.PoiTagCounts;
import com.example.backend.repositories.DemographicsRepository;
import com.example.backend.repositories.H3HexagonRepository;
import com.example.backend.repositories.PoiRepository;
import com.example.backend.services.admin.ScoreCacheVersionService;
import com.uber.h3core.H3Core;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class FootTrafficRecomputeService {

    private static final int SCENARIO_DEFAULT = 0;
    private static final String FALLBACK_ARCHETYPE = "RESIDENTIAL";

    private final FootTrafficProperties properties;
    private final H3HexagonRepository h3HexagonRepository;
    private final PoiRepository poiRepository;
    private final DemographicsRepository demographicsRepository;
    private final FootTrafficCellProfileRepository cellProfileRepository;
    private final FootTrafficZoneParamsRepository zoneParamsRepository;
    private final FootTrafficService footTrafficService;
    private final ScoreCacheVersionService scoreCacheVersionService;
    private final H3Core h3;

    public record RecomputeResult(int cellsProcessed, long durationMs, long trafficVersion) {}

    public RecomputeResult recomputeAll(Integer scenarioId, String h3Prefix) {
        if (!properties.isEnabled()) {
            log.info("Foot-traffic recompute skipped: disabled");
            return new RecomputeResult(0, 0, scoreCacheVersionService.getTrafficVersion());
        }
        long t0 = System.nanoTime();
        long trafficV = scoreCacheVersionService.bumpTrafficVersion();

        int batchSize = Math.max(100, properties.getRecompute().getBatchSize());
        int processed = 0;
        int page = 0;
        List<String> batchH3 = new ArrayList<>();
        List<Long> batchPeaks = new ArrayList<>();

        while (true) {
            Page<com.example.backend.entities.H3Hexagon> pg =
                    h3HexagonRepository.findAll(PageRequest.of(page, batchSize, Sort.by("h3Index")));
            if (pg.isEmpty()) {
                break;
            }
            for (com.example.backend.entities.H3Hexagon hex : pg.getContent()) {
                String h3Index = hex.getH3Index();
                if (h3Prefix != null && !h3Prefix.isBlank() && !h3Index.startsWith(h3Prefix.trim())) {
                    continue;
                }
                processOneCell(h3Index, trafficV, batchH3, batchPeaks);
                processed++;
            }
            if (!pg.hasNext()) {
                break;
            }
            page++;
        }

        flushPeakBatch(trafficV, batchH3, batchPeaks);
        applyRuralImputation(h3Prefix);

        long ms = (System.nanoTime() - t0) / 1_000_000L;
        log.info("Foot-traffic recompute: {} cell(s) in {} ms (trafficVersion={})", processed, ms, trafficV);
        return new RecomputeResult(processed, ms, trafficV);
    }

    private void processOneCell(
            String h3Index, long trafficV, List<String> batchH3, List<Long> batchPeaks) {
        List<Object[]> rows = poiRepository.countTypeTagsGroupedWithinHex(h3Index);
        PoiTagCounts counts = PoiFootTrafficTagAggregator.aggregate(rows);
        double pop = 0;
        double income = 0;
        Optional<Demographics> demo = demographicsRepository.findById(h3Index);
        if (demo.isPresent()) {
            if (demo.get().getPopulationDensity() != null) {
                pop = demo.get().getPopulationDensity();
            }
            if (demo.get().getAvgIncome() != null) {
                income = demo.get().getAvgIncome();
            }
        }

        ArchetypeResult arch = FootTrafficZoneClassifier.classify(counts, pop);
        var params =
                zoneParamsRepository
                        .findById(arch.archetype())
                        .orElseGet(
                                () ->
                                        zoneParamsRepository
                                                .findById(FALLBACK_ARCHETYPE)
                                                .orElseThrow());

        BaselineResult baseline =
                FootTrafficBaselineCalculator.calc(
                        counts,
                        params,
                        pop,
                        income,
                        h3Index,
                        properties.getJitterSalt());

        int driverTotal = (int) Math.min(Integer.MAX_VALUE, counts.total());
        int comp = (int) Math.min(Integer.MAX_VALUE, counts.competitorApprox());
        int transit = (int) Math.min(Integer.MAX_VALUE, counts.transitPoiCount() + counts.railwayStationCount());

        cellProfileRepository.upsert(
                h3Index,
                SCENARIO_DEFAULT,
                arch.archetype(),
                arch.confidence(),
                baseline.baselineDaily(),
                baseline.peakHourly(),
                driverTotal,
                comp,
                transit,
                pop,
                income,
                baseline.noiseSeed());

        batchH3.add(h3Index);
        batchPeaks.add((long) baseline.peakHourly());
        if (batchH3.size() >= 500) {
            flushPeakBatch(trafficV, batchH3, batchPeaks);
        }
    }

    private void flushPeakBatch(long trafficV, List<String> batchH3, List<Long> batchPeaks) {
        if (batchH3.isEmpty()) {
            return;
        }
        footTrafficService.writePeakCachePipelined(trafficV, new ArrayList<>(batchH3), new ArrayList<>(batchPeaks));
        batchH3.clear();
        batchPeaks.clear();
    }

    /** Optional: lift isolated rural baselines toward ring-1 average. */
    private void applyRuralImputation(String h3Prefix) {
        List<String> all =
                h3HexagonRepository.findAll().stream()
                        .map(com.example.backend.entities.H3Hexagon::getH3Index)
                        .filter(
                                h ->
                                        h3Prefix == null
                                                || h3Prefix.isBlank()
                                                || h.startsWith(h3Prefix.trim()))
                        .toList();
        for (String h3Index : all) {
            var prof = cellProfileRepository.findById(h3Index);
            if (prof.isEmpty()) {
                continue;
            }
            if (!"RURAL".equals(prof.get().getArchetype()) || prof.get().getDriverPoiCount() >= 3) {
                continue;
            }
            Double avg = neighbourAvgBaseline(h3Index);
            if (avg == null || avg <= prof.get().getBaselineDaily()) {
                continue;
            }
            int newBase = (int) Math.round(avg);
            var params =
                    zoneParamsRepository
                            .findById("RURAL")
                            .orElse(zoneParamsRepository.findById(FALLBACK_ARCHETYPE).orElseThrow());
            double maxWd =
                    java.util.Arrays.stream(params.getHourlyCurveWd())
                            .mapToDouble(x -> x != null ? x : 0)
                            .max()
                            .orElse(1.0);
            int newPeak = (int) Math.round(newBase * (maxWd / 24.0));
            cellProfileRepository.upsert(
                    h3Index,
                    SCENARIO_DEFAULT,
                    prof.get().getArchetype(),
                    prof.get().getArchetypeConfidence(),
                    newBase,
                    newPeak,
                    prof.get().getDriverPoiCount(),
                    prof.get().getCompetitorPoiCount(),
                    prof.get().getTransitPoiCount(),
                    prof.get().getPopDensity(),
                    prof.get().getAvgIncome(),
                    prof.get().getNoiseSeed());
            long tv = scoreCacheVersionService.getTrafficVersion();
            footTrafficService.writePeakCache(h3Index, newPeak);
        }
    }

    private Double neighbourAvgBaseline(String h3Index) {
        try {
            long center = h3.stringToH3(h3Index);
            List<Long> disk = h3.gridDisk(center, 1);
            double sum = 0;
            int n = 0;
            for (Long cell : disk) {
                String s = h3.h3ToString(cell);
                if (s.equals(h3Index)) {
                    continue;
                }
                Optional<com.example.backend.foottraffic.entities.FootTrafficCellProfile> p =
                        cellProfileRepository.findById(s);
                if (p.isPresent()) {
                    sum += p.get().getBaselineDaily();
                    n++;
                }
            }
            return n > 0 ? sum / n : null;
        } catch (Exception e) {
            return null;
        }
    }
}
