package com.example.backend.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import com.example.backend.controllers.dto.PoiMapResponse;
import com.example.backend.entities.Poi;
import com.example.backend.repositories.PoiRepository;
import com.example.backend.scoring.ScoringStrategy;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PoiServiceTest {

    @Mock
    private PoiRepository poiRepository;

    @Mock
    private ScoringStrategy scoringStrategy;

    @InjectMocks
    private PoiService poiService;

    @Test
    void getPoisInBoundingBox_mapsToResponsesWithScore() {
        GeometryFactory fac = new GeometryFactory(new PrecisionModel(), 4326);
        Poi p = new Poi();
        p.setId(UUID.randomUUID());
        p.setName("Cafe");
        p.setTypeTag("category=cafe");
        p.setLocation(fac.createPoint(new Coordinate(-7.6, 33.5)));

        ReflectionTestUtils.setField(poiService, "driverCategoriesConfig", "category=cafe");
        ReflectionTestUtils.setField(poiService, "radiusDriversKm", 2.0);
        ReflectionTestUtils.setField(poiService, "radiusCompetitorsKm", 1.0);
        ReflectionTestUtils.setField(poiService, "maxDensityCount", 50);
        ReflectionTestUtils.setField(poiService, "maxBboxDeg", 0.5);

        when(poiRepository.findAllInBoundingBox(0, 0, 0.4, 0.4)).thenReturn(List.of(p));
        when(poiRepository.countByTypeTagAndNearby("category=cafe", 33.5, -7.6, 2000.0))
                .thenReturn(0L);
        when(poiRepository.countByTypeTagAndNearby("category=cafe", 33.5, -7.6, 1000.0))
                .thenReturn(1L);
        when(poiRepository.countAllNearby(33.5, -7.6, 2000.0)).thenReturn(0L);
        when(scoringStrategy.computeSaturationScore(0, 0, 0.0))
                .thenReturn(CompletableFuture.completedFuture(77.0));

        List<PoiMapResponse> out = poiService.getPoisInBoundingBox(0, 0, 0.4, 0.4);
        assertEquals(1, out.size());
        assertEquals(77.0, out.get(0).saturationScore());
    }

    @Test
    void getPoisInBoundingBox_scoreNullOnStrategyFailure() {
        GeometryFactory fac = new GeometryFactory(new PrecisionModel(), 4326);
        Poi p = new Poi();
        p.setId(UUID.randomUUID());
        p.setName("X");
        p.setTypeTag("category=shop");
        p.setLocation(fac.createPoint(new Coordinate(0, 0)));

        ReflectionTestUtils.setField(poiService, "driverCategoriesConfig", "category=shop");
        ReflectionTestUtils.setField(poiService, "radiusDriversKm", 2.0);
        ReflectionTestUtils.setField(poiService, "radiusCompetitorsKm", 1.0);
        ReflectionTestUtils.setField(poiService, "maxDensityCount", 50);
        ReflectionTestUtils.setField(poiService, "maxBboxDeg", 0.5);

        when(poiRepository.findAllInBoundingBox(0, 0, 0.4, 0.4)).thenReturn(List.of(p));
        when(poiRepository.countByTypeTagAndNearby("category=shop", 0, 0, 2000.0)).thenReturn(0L);
        when(poiRepository.countByTypeTagAndNearby("category=shop", 0, 0, 1000.0)).thenReturn(1L);
        when(poiRepository.countAllNearby(0, 0, 2000.0)).thenReturn(0L);
        when(scoringStrategy.computeSaturationScore(0, 0, 0.0))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("x")));

        List<PoiMapResponse> out = poiService.getPoisInBoundingBox(0, 0, 0.4, 0.4);
        assertEquals(1, out.size());
        assertNull(out.get(0).saturationScore());
    }
}
