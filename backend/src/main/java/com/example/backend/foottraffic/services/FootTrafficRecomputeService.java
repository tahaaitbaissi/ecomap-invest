package com.example.backend.foottraffic.services;

import com.ecomap.foottraffic.simulation.FootTrafficSimulationEngine;
import com.ecomap.foottraffic.simulation.FootTrafficZoneParamsSnapshot;
import com.ecomap.foottraffic.simulation.PoiFootTrafficTagAggregator;
import com.ecomap.foottraffic.simulation.PoiTagCounts;
import com.ecomap.ftsim.ws.xml.SimulateCellRequest;
import com.ecomap.ftsim.ws.xml.TagCountRow;
import com.example.backend.demographics.DeterministicDemographicsFallback;
import com.example.backend.entities.Demographics;
import com.example.backend.foottraffic.config.FootTrafficProperties;
import com.example.backend.foottraffic.repositories.FootTrafficCellProfileRepository;
import com.example.backend.foottraffic.repositories.FootTrafficZoneParamsRepository;
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
    private final FootTrafficSoapSimulationClient soapSimulationClient;
    private final H3Core h3;
    private final DeterministicDemographicsFallback demographicsFallback;

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
        Optional<Demographics> demo = demographicsRepository.findById(h3Index);
        double pop = demographicsFallback.populationDensity(demo, h3Index);
        double income = demographicsFallback.avgIncome(demo, h3Index);

        FootTrafficSimulationEngine.CellSimulationResult result = computeCellSimulation(h3Index, rows, pop, income);

        cellProfileRepository.upsert(
                h3Index,
                SCENARIO_DEFAULT,
                result.archetype(),
                result.archetypeConfidence(),
                result.baselineDaily(),
                result.peakHourly(),
                result.driverPoiCount(),
                result.competitorPoiCount(),
                result.transitPoiCount(),
                pop,
                income,
                result.noiseSeed());

        batchH3.add(h3Index);
        batchPeaks.add((long) result.peakHourly());
        if (batchH3.size() >= 500) {
            flushPeakBatch(trafficV, batchH3, batchPeaks);
        }
    }

    private FootTrafficSimulationEngine.CellSimulationResult computeCellSimulation(
            String h3Index, List<Object[]> rows, double pop, double income) {
        if (soapSimulationClient.isReady()) {
            try {
                SimulateCellRequest rq = buildSoapRequest(h3Index, rows, pop, income);
                FootTrafficSimulationEngine.CellSimulationResult remote =
                        soapSimulationClient.simulateCellAsync(rq).join();
                if (remote != null) {
                    return remote;
                }
            } catch (Exception ex) {
                log.debug("SOAP foot-traffic failed, local engine: {}", ex.getMessage());
            }
        }
        return simulateLocal(h3Index, rows, pop, income);
    }

    private SimulateCellRequest buildSoapRequest(
            String h3Index, List<Object[]> rows, double pop, double income) {
        SimulateCellRequest rq = new SimulateCellRequest();
        rq.setH3Index(h3Index);
        rq.setScenarioId(SCENARIO_DEFAULT);
        rq.setJitterSalt(properties.getJitterSalt());
        rq.setPopulationDensity(pop);
        rq.setAvgIncome(income);
        for (Object[] row : rows) {
            if (row == null || row.length < 2) {
                continue;
            }
            TagCountRow tr = new TagCountRow();
            tr.setTypeTag(row[0] != null ? row[0].toString() : "");
            tr.setCount(toLong(row[1]));
            rq.getTagRow().add(tr);
        }
        return rq;
    }

    private static long toLong(Object o) {
        if (o == null) {
            return 0L;
        }
        if (o instanceof Number n) {
            return n.longValue();
        }
        try {
            return Long.parseLong(o.toString());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private FootTrafficSimulationEngine.CellSimulationResult simulateLocal(
            String h3Index, List<Object[]> rows, double pop, double income) {
        PoiTagCounts counts = PoiFootTrafficTagAggregator.aggregate(rows);
        return FootTrafficSimulationEngine.simulate(
                counts, pop, income, h3Index, properties.getJitterSalt(), this::resolveZoneSnapshot);
    }

    private FootTrafficZoneParamsSnapshot resolveZoneSnapshot(String archetype) {
        return zoneParamsRepository
                .findById(archetype)
                .orElseGet(() -> zoneParamsRepository.findById(FALLBACK_ARCHETYPE).orElseThrow())
                .toSnapshot();
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
