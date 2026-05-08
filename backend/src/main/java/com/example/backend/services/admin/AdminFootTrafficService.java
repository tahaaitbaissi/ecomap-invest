package com.example.backend.services.admin;

import com.example.backend.controllers.dto.foottraffic.AdminFootTrafficParamsResponse;
import com.example.backend.controllers.dto.foottraffic.AdminFootTrafficParamsUpsertRequest;
import com.example.backend.controllers.dto.foottraffic.FootTrafficAuditResponse;
import com.example.backend.controllers.dto.foottraffic.FootTrafficRecomputeApiResponse;
import com.example.backend.foottraffic.entities.FootTrafficZoneParams;
import com.example.backend.foottraffic.repositories.FootTrafficCellProfileRepository;
import com.example.backend.foottraffic.repositories.FootTrafficZoneParamsRepository;
import com.example.backend.foottraffic.services.FootTrafficRecomputeService;
import com.example.backend.foottraffic.simulation.DayType;
import com.example.backend.foottraffic.simulation.FootTrafficHourlyCurveGenerator;
import com.example.backend.repositories.PoiRepository;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminFootTrafficService {

    private final FootTrafficZoneParamsRepository zoneParamsRepository;
    private final FootTrafficCellProfileRepository cellProfileRepository;
    private final FootTrafficRecomputeService footTrafficRecomputeService;
    private final ScoreCacheVersionService scoreCacheVersionService;
    private final PoiRepository poiRepository;

    @Transactional(readOnly = true)
    public List<AdminFootTrafficParamsResponse> listParams() {
        return zoneParamsRepository.findAll().stream()
                .sorted(Comparator.comparing(FootTrafficZoneParams::getArchetype))
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminFootTrafficParamsResponse getParams(String archetype) {
        FootTrafficZoneParams e =
                zoneParamsRepository
                        .findById(archetype.trim().toUpperCase())
                        .orElseThrow(() -> new NoSuchElementException("Unknown archetype: " + archetype));
        return toResponse(e);
    }

    @Transactional
    public AdminFootTrafficParamsResponse upsert(String archetype, AdminFootTrafficParamsUpsertRequest req) {
        String key = archetype.trim().toUpperCase();
        validateFinite(req);
        FootTrafficZoneParams e = zoneParamsRepository.findById(key).orElseGet(FootTrafficZoneParams::new);
        e.setArchetype(key);
        e.setBaseDailyMin(req.baseDailyMin());
        e.setBaseDailyMax(req.baseDailyMax());
        e.setPoiDensityCap(req.poiDensityCap());
        e.setPopDensityCap(req.popDensityCap());
        e.setIncomeWeight(req.incomeWeight());
        e.setHourlyCurveWd(listToArray(req.hourlyCurveWd()));
        e.setHourlyCurveSat(listToArray(req.hourlyCurveSat()));
        e.setHourlyCurveSun(listToArray(req.hourlyCurveSun()));
        e.setSeasonalScalers(listToArray12(req.seasonalScalers()));
        e.setDayScalerSat(req.dayScalerSat());
        e.setDayScalerSun(req.dayScalerSun());
        e.setNoiseSigma(req.noiseSigma());
        e.setUpdatedAt(Timestamp.from(Instant.now()));
        AdminFootTrafficParamsResponse out = toResponse(zoneParamsRepository.save(e));
        scoreCacheVersionService.bumpTrafficVersion();
        return out;
    }

    public FootTrafficRecomputeApiResponse recompute(Integer scenarioId, String h3Prefix) {
        var r = footTrafficRecomputeService.recomputeAll(scenarioId != null ? scenarioId : 0, h3Prefix);
        return new FootTrafficRecomputeApiResponse(r.cellsProcessed(), r.durationMs(), r.trafficVersion());
    }

    @Transactional(readOnly = true)
    public FootTrafficAuditResponse audit(String h3Index) {
        var prof =
                cellProfileRepository
                        .findById(h3Index)
                        .orElseThrow(() -> new NoSuchElementException("No foot-traffic profile for " + h3Index));
        var params =
                zoneParamsRepository
                        .findById(prof.getArchetype())
                        .orElseThrow(() -> new NoSuchElementException("Zone params missing: " + prof.getArchetype()));

        List<Object[]> rows = poiRepository.countTypeTagsGroupedWithinHex(h3Index);
        Map<String, Long> sample = new LinkedHashMap<>();
        int i = 0;
        for (Object[] row : rows) {
            if (i++ >= 80) {
                break;
            }
            if (row.length >= 2 && row[0] != null) {
                sample.put(row[0].toString(), ((Number) row[1]).longValue());
            }
        }

        int monthJune = 5;
        double[] wd =
                FootTrafficHourlyCurveGenerator.generate(params, DayType.WD, monthJune, prof.getBaselineDaily());
        double[] sat =
                FootTrafficHourlyCurveGenerator.generate(params, DayType.SAT, monthJune, prof.getBaselineDaily());
        double[] sun =
                FootTrafficHourlyCurveGenerator.generate(params, DayType.SUN, monthJune, prof.getBaselineDaily());

        double seasonal = 1.0;
        if (params.getSeasonalScalers() != null && params.getSeasonalScalers().length > monthJune) {
            Double s = params.getSeasonalScalers()[monthJune];
            if (s != null) {
                seasonal = s;
            }
        }

        return new FootTrafficAuditResponse(
                h3Index,
                prof.getArchetype(),
                prof.getArchetypeConfidence(),
                prof.getBaselineDaily(),
                prof.getPeakHourly(),
                prof.getDriverPoiCount(),
                prof.getCompetitorPoiCount(),
                prof.getTransitPoiCount(),
                prof.getPopDensity(),
                prof.getAvgIncome(),
                prof.getNoiseSeed(),
                prof.getComputedAt() != null ? prof.getComputedAt().toInstant().toString() : null,
                sample,
                toList(wd),
                toList(sat),
                toList(sun),
                seasonal);
    }

    private static void validateFinite(AdminFootTrafficParamsUpsertRequest req) {
        if (!Double.isFinite(req.poiDensityCap()) || !Double.isFinite(req.popDensityCap())) {
            throw new IllegalArgumentException("Caps must be finite");
        }
    }

    private AdminFootTrafficParamsResponse toResponse(FootTrafficZoneParams e) {
        return new AdminFootTrafficParamsResponse(
                e.getArchetype(),
                e.getBaseDailyMin(),
                e.getBaseDailyMax(),
                e.getPoiDensityCap(),
                e.getPopDensityCap(),
                e.getIncomeWeight(),
                toList(e.getHourlyCurveWd()),
                toList(e.getHourlyCurveSat()),
                toList(e.getHourlyCurveSun()),
                toList12(e.getSeasonalScalers()),
                e.getDayScalerSat(),
                e.getDayScalerSun(),
                e.getNoiseSigma(),
                e.getUpdatedAt() != null ? e.getUpdatedAt().toInstant().toString() : null);
    }

    private static Double[] listToArray(List<Double> list) {
        Double[] a = new Double[24];
        for (int i = 0; i < 24; i++) {
            a[i] = list.get(i);
        }
        return a;
    }

    private static Double[] listToArray12(List<Double> list) {
        Double[] a = new Double[12];
        for (int i = 0; i < 12; i++) {
            a[i] = list.get(i);
        }
        return a;
    }

    private static List<Double> toList(Double[] arr) {
        List<Double> out = new ArrayList<>(24);
        if (arr == null) {
            return out;
        }
        for (Double d : arr) {
            out.add(d != null ? d : 0.0);
        }
        return out;
    }

    private static List<Double> toList12(Double[] arr) {
        List<Double> out = new ArrayList<>(12);
        if (arr == null) {
            return out;
        }
        for (Double d : arr) {
            out.add(d != null ? d : 1.0);
        }
        return out;
    }

    private static List<Double> toList(double[] arr) {
        List<Double> out = new ArrayList<>(arr.length);
        for (double v : arr) {
            out.add(v);
        }
        return out;
    }
}
