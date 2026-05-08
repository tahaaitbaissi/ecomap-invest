package com.example.backend.foottraffic.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.backend.entities.H3Hexagon;
import com.example.backend.foottraffic.config.FootTrafficProperties;
import com.example.backend.foottraffic.entities.FootTrafficCellProfile;
import com.example.backend.foottraffic.entities.FootTrafficZoneParams;
import com.example.backend.foottraffic.repositories.FootTrafficCellProfileRepository;
import com.example.backend.foottraffic.repositories.FootTrafficZoneParamsRepository;
import com.example.backend.repositories.DemographicsRepository;
import com.example.backend.repositories.H3HexagonRepository;
import com.example.backend.repositories.PoiRepository;
import com.example.backend.services.admin.ScoreCacheVersionService;
import com.uber.h3core.H3Core;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

class FootTrafficRecomputeServiceTest {

    private static FootTrafficProperties props() {
        var p = new FootTrafficProperties();
        p.setEnabled(true);
        p.getRecompute().setBatchSize(100);
        return p;
    }

    private static FootTrafficZoneParams ruralParams() {
        var p = new FootTrafficZoneParams();
        p.setArchetype("RURAL");
        p.setBaseDailyMin(10);
        p.setBaseDailyMax(100);
        p.setPoiDensityCap(10.0);
        p.setPopDensityCap(1000.0);
        p.setIncomeWeight(0.0);
        p.setNoiseSigma(0.0);
        p.setDayScalerSat(1.0);
        p.setDayScalerSun(1.0);
        p.setSeasonalScalers(new Double[] {1d, 1d, 1d, 1d, 1d, 1d, 1d, 1d, 1d, 1d, 1d, 1d});
        Double[] curve = new Double[24];
        for (int i = 0; i < 24; i++) curve[i] = 1d;
        curve[12] = 2d; // max 2
        p.setHourlyCurveWd(curve);
        p.setHourlyCurveSat(curve);
        p.setHourlyCurveSun(curve);
        return p;
    }

    @Test
    void recomputeAll_bumps_version_upserts_and_warms_redis() {
        var properties = props();
        var h3HexRepo = Mockito.mock(H3HexagonRepository.class);
        var poiRepo = Mockito.mock(PoiRepository.class);
        var demoRepo = Mockito.mock(DemographicsRepository.class);
        var cellRepo = Mockito.mock(FootTrafficCellProfileRepository.class);
        var zoneRepo = Mockito.mock(FootTrafficZoneParamsRepository.class);
        var footTrafficService = Mockito.mock(FootTrafficService.class);
        var versions = Mockito.mock(ScoreCacheVersionService.class);
        var h3 = Mockito.mock(H3Core.class);

        when(versions.bumpTrafficVersion()).thenReturn(5L);
        when(versions.getTrafficVersion()).thenReturn(5L);
        when(zoneRepo.findById("RURAL")).thenReturn(Optional.of(ruralParams()));
        when(zoneRepo.findById("RESIDENTIAL")).thenReturn(Optional.of(ruralParams()));

        var a = new H3Hexagon();
        a.setH3Index("a");
        var b = new H3Hexagon();
        b.setH3Index("b");

        when(h3HexRepo.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(a, b)));
        when(h3HexRepo.findAll()).thenReturn(List.of(a, b));

        when(poiRepo.countTypeTagsGroupedWithinHex(any())).thenReturn(List.of());
        when(demoRepo.findById(any())).thenReturn(Optional.empty());
        when(cellRepo.findById(any())).thenReturn(Optional.empty()); // skip imputation

        var svc =
                new FootTrafficRecomputeService(
                        properties,
                        h3HexRepo,
                        poiRepo,
                        demoRepo,
                        cellRepo,
                        zoneRepo,
                        footTrafficService,
                        versions,
                        h3);

        var res = svc.recomputeAll(null, null);

        assertThat(res.cellsProcessed()).isEqualTo(2);
        assertThat(res.trafficVersion()).isEqualTo(5L);
        verify(versions).bumpTrafficVersion();
        verify(cellRepo, Mockito.times(2))
                .upsert(
                        any(),
                        anyInt(),
                        any(),
                        anyDouble(),
                        anyInt(),
                        anyInt(),
                        anyInt(),
                        anyInt(),
                        anyInt(),
                        anyDouble(),
                        anyDouble(),
                        anyLong());
        verify(footTrafficService).writePeakCachePipelined(eq(5L), any(), any());
    }

    @Test
    void recomputeAll_rural_imputation_lifts_isolated_rural_baseline() throws Exception {
        var properties = props();
        var h3HexRepo = Mockito.mock(H3HexagonRepository.class);
        var poiRepo = Mockito.mock(PoiRepository.class);
        var demoRepo = Mockito.mock(DemographicsRepository.class);
        var cellRepo = Mockito.mock(FootTrafficCellProfileRepository.class);
        var zoneRepo = Mockito.mock(FootTrafficZoneParamsRepository.class);
        var footTrafficService = Mockito.mock(FootTrafficService.class);
        var versions = Mockito.mock(ScoreCacheVersionService.class);
        var h3 = Mockito.mock(H3Core.class);

        when(versions.bumpTrafficVersion()).thenReturn(1L);
        when(versions.getTrafficVersion()).thenReturn(9L);
        when(zoneRepo.findById("RURAL")).thenReturn(Optional.of(ruralParams()));
        when(zoneRepo.findById("RESIDENTIAL")).thenReturn(Optional.of(ruralParams()));

        var centerHex = new H3Hexagon();
        centerHex.setH3Index("center");
        when(h3HexRepo.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(centerHex)));
        when(h3HexRepo.findAll()).thenReturn(List.of(centerHex));
        when(poiRepo.countTypeTagsGroupedWithinHex(any())).thenReturn(List.of());
        when(demoRepo.findById(any())).thenReturn(Optional.empty());

        var centerProf = new FootTrafficCellProfile();
        centerProf.setH3Index("center");
        centerProf.setArchetype("RURAL");
        centerProf.setDriverPoiCount(0);
        centerProf.setBaselineDaily(100);
        centerProf.setPeakHourly(8);
        centerProf.setArchetypeConfidence(0.5);
        centerProf.setCompetitorPoiCount(0);
        centerProf.setTransitPoiCount(0);
        centerProf.setPopDensity(0.0);
        centerProf.setAvgIncome(0.0);
        centerProf.setNoiseSeed(1L);

        var neighborProf = new FootTrafficCellProfile();
        neighborProf.setH3Index("neighbor");
        neighborProf.setBaselineDaily(200);

        when(cellRepo.findById("center")).thenReturn(Optional.of(centerProf));
        when(cellRepo.findById("neighbor")).thenReturn(Optional.of(neighborProf));

        when(h3.stringToH3("center")).thenReturn(1L);
        when(h3.gridDisk(1L, 1)).thenReturn(List.of(1L, 2L));
        when(h3.h3ToString(2L)).thenReturn("neighbor");
        when(h3.h3ToString(1L)).thenReturn("center");

        var svc =
                new FootTrafficRecomputeService(
                        properties,
                        h3HexRepo,
                        poiRepo,
                        demoRepo,
                        cellRepo,
                        zoneRepo,
                        footTrafficService,
                        versions,
                        h3);

        svc.recomputeAll(null, null);

        ArgumentCaptor<Integer> baseline = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Integer> peak = ArgumentCaptor.forClass(Integer.class);
        verify(cellRepo, Mockito.times(2))
                .upsert(
                        eq("center"),
                        anyInt(),
                        eq("RURAL"),
                        anyDouble(),
                        baseline.capture(),
                        peak.capture(),
                        anyInt(),
                        anyInt(),
                        anyInt(),
                        anyDouble(),
                        anyDouble(),
                        anyLong());

        int imputedBaseline = baseline.getAllValues().get(1);
        int imputedPeak = peak.getAllValues().get(1);
        assertThat(imputedBaseline).isEqualTo(200);
        assertThat(imputedPeak).isEqualTo((int) Math.round(200 * (2.0 / 24.0)));
        verify(footTrafficService).writePeakCache(eq("center"), eq((long) imputedPeak));
        verify(footTrafficService, never()).writePeakCache(eq("neighbor"), anyLong());
    }
}

