package com.example.backend.services;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.backend.controllers.dto.HexagonMapResponse;
import com.example.backend.entities.DynamicProfile;
import com.example.backend.repositories.DemographicsRepository;
import com.example.backend.repositories.DynamicProfileRepository;
import com.example.backend.repositories.PoiRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.uber.h3core.H3Core;
import com.uber.h3core.util.LatLng;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

class HexagonScoringServiceTest {

    @Test
    void minMaxOrNeutral_allEqual_defaultsTo50InLoop() {
        assertArrayEquals(
                new double[] {0, 0, 1.0},
                HexagonScoringService.minMaxOrNeutral(List.of()),
                0.001);
        assertArrayEquals(
                new double[] {42, 42, 1.0},
                HexagonScoringService.minMaxOrNeutral(List.of(42.0, 42.0, 42.0)),
                0.001);
        assertArrayEquals(
                new double[] {10, 20, 0.0},
                HexagonScoringService.minMaxOrNeutral(List.of(10.0, 20.0)),
                0.001);
    }

    @Test
    void parseBbox_inheritedViaGetHexagons_missingArg() {
        PoiRepository poi = mock(PoiRepository.class);
        DynamicProfileRepository dp = mock(DynamicProfileRepository.class);
        DemographicsRepository dem = mock(DemographicsRepository.class);
        H3Core h3 = mock(H3Core.class);
        HexagonScoringService svc = new HexagonScoringService(h3, poi, dp, dem, null);
        wireDefaults(svc);

        assertThrows(
                IllegalArgumentException.class, () -> svc.getHexagonsInBbox(null, null));
        verify(h3, org.mockito.Mockito.never())
                .polygonToCells(anyList(), anyList(), anyInt());
    }

    @Test
    void getHexagons_tinyBbox_oneCell() {
        PoiRepository poi = mock(PoiRepository.class);
        DynamicProfileRepository dp = mock(DynamicProfileRepository.class);
        DemographicsRepository dem = mock(DemographicsRepository.class);
        H3Core h3 = mock(H3Core.class);
        long fakeCell = 0x891ea6c0d47ffffL;
        when(h3.polygonToCells(anyList(), anyList(), anyInt())).thenReturn(List.of(fakeCell));
        when(h3.h3ToString(fakeCell)).thenReturn("891ea6c0d47ffff");
        when(h3.stringToH3("891ea6c0d47ffff")).thenReturn(fakeCell);
        when(h3.cellToLatLng(fakeCell)).thenReturn(new LatLng(33.57, -7.59));
        when(h3.cellToBoundary(fakeCell))
                .thenReturn(
                        List.of(
                                new LatLng(33.56, -7.60),
                                new LatLng(33.57, -7.58),
                                new LatLng(33.58, -7.59)));
        when(poi.countByTypeTagAndNearby(anyString(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(0L);
        when(poi.countAllNearby(anyDouble(), anyDouble(), anyDouble())).thenReturn(0L);
        HexagonScoringService svc = new HexagonScoringService(h3, poi, dp, dem, null);
        wireDefaults(svc);
        String bbox = "-7.59,33.57,-7.57,33.59";
        List<HexagonMapResponse> r = svc.getHexagonsInBbox(bbox, null);
        assertTrue(r.size() == 1);
        assertTrue(r.get(0).h3Index().length() > 0);
        assertTrue(r.get(0).boundary().size() >= 3);
    }

    @Test
    void getHexagons_unknownProfile_404() {
        PoiRepository poi = mock(PoiRepository.class);
        DynamicProfileRepository dp = mock(DynamicProfileRepository.class);
        DemographicsRepository dem = mock(DemographicsRepository.class);
        H3Core h3 = mock(H3Core.class);
        when(dp.findById(any(UUID.class))).thenReturn(Optional.empty());
        HexagonScoringService svc = new HexagonScoringService(h3, poi, dp, dem, null);
        wireDefaults(svc);
        String bbox = "-7.59,33.57,-7.57,33.59";
        assertThrows(
                java.util.NoSuchElementException.class,
                () -> svc.getHexagonsInBbox(bbox, UUID.randomUUID()));
        verify(h3, org.mockito.Mockito.never()).polygonToCells(anyList(), anyList(), anyInt());
    }

    @Test
    void getHexagons_withProfile_parsesJson() {
        PoiRepository poi = mock(PoiRepository.class);
        DynamicProfileRepository dp = mock(DynamicProfileRepository.class);
        DemographicsRepository dem = mock(DemographicsRepository.class);
        H3Core h3 = mock(H3Core.class);
        long fakeCell = 0x891ea6c0d47ffffL;
        when(h3.polygonToCells(anyList(), anyList(), anyInt())).thenReturn(List.of(fakeCell));
        when(h3.h3ToString(fakeCell)).thenReturn("891ea6c0d47ffff");
        when(h3.stringToH3("891ea6c0d47ffff")).thenReturn(fakeCell);
        when(h3.cellToLatLng(fakeCell)).thenReturn(new LatLng(33.57, -7.59));
        when(h3.cellToBoundary(fakeCell))
                .thenReturn(
                        List.of(
                                new LatLng(33.56, -7.60),
                                new LatLng(33.57, -7.58),
                                new LatLng(33.58, -7.59)));
        ObjectMapper om = new ObjectMapper();
        ArrayNode drivers = om.createArrayNode();
        ObjectNode d1 = om.createObjectNode();
        d1.put("tag", "category=school");
        d1.put("weight", 1.0);
        drivers.add(d1);
        ArrayNode comp = om.createArrayNode();
        ObjectNode c1 = om.createObjectNode();
        c1.put("tag", "amenity=cafe");
        c1.put("weight", 0.5);
        comp.add(c1);
        DynamicProfile prof = new DynamicProfile();
        prof.setId(UUID.randomUUID());
        prof.setUserId(1L);
        prof.setUserQuery("q");
        prof.setDriversConfig(drivers);
        prof.setCompetitorsConfig(comp);
        when(dp.findById(prof.getId())).thenReturn(Optional.of(prof));
        when(poi.countByTypeTagAndNearby(anyString(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(0L);
        when(poi.countAllNearby(anyDouble(), anyDouble(), anyDouble())).thenReturn(0L);
        HexagonScoringService svc = new HexagonScoringService(h3, poi, dp, dem, null);
        wireDefaults(svc);
        String bbox = "-7.59,33.57,-7.57,33.59";
        List<HexagonMapResponse> r = svc.getHexagonsInBbox(bbox, prof.getId());
        assertTrue(!r.isEmpty());
    }

    @Test
    void getHexagons_cacheHit_usesRawFromRedis() {
        PoiRepository poi = mock(PoiRepository.class);
        DynamicProfileRepository dp = mock(DynamicProfileRepository.class);
        DemographicsRepository dem = mock(DemographicsRepository.class);
        H3Core h3 = mock(H3Core.class);
        long fakeCell = 0x891ea6c0d47ffffL;
        when(h3.polygonToCells(anyList(), anyList(), anyInt())).thenReturn(List.of(fakeCell));
        when(h3.h3ToString(fakeCell)).thenReturn("891ea6c0d47ffff");
        when(h3.stringToH3("891ea6c0d47ffff")).thenReturn(fakeCell);
        when(h3.cellToLatLng(fakeCell)).thenReturn(new LatLng(33.57, -7.59));
        when(h3.cellToBoundary(fakeCell))
                .thenReturn(
                        List.of(
                                new LatLng(33.56, -7.60),
                                new LatLng(33.57, -7.58),
                                new LatLng(33.58, -7.59)));
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> vops = mock(ValueOperations.class);
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        when(redis.opsForValue()).thenReturn(vops);
        when(vops.multiGet(any()))
                .thenReturn(List.of("0.3"));

        HexagonScoringService svc = new HexagonScoringService(h3, poi, dp, dem, redis);
        wireDefaults(svc);
        String bbox = "-7.59,33.57,-7.57,33.59";
        List<HexagonMapResponse> r = svc.getHexagonsInBbox(bbox, null);
        assertTrue(r.size() == 1);
        verify(poi, org.mockito.Mockito.never())
                .countByTypeTagAndNearby(anyString(), anyDouble(), anyDouble(), anyDouble());
    }

    void wireDefaults(HexagonScoringService svc) {
        ReflectionTestUtils.setField(svc, "driverCategoriesConfig", "category=school");
        ReflectionTestUtils.setField(svc, "radiusDriversKm", 2.0);
        ReflectionTestUtils.setField(svc, "radiusCompetitorsKm", 1.0);
        ReflectionTestUtils.setField(svc, "maxDensityCount", 50);
        ReflectionTestUtils.setField(svc, "h3Resolution", 9);
        ReflectionTestUtils.setField(svc, "maxCells", 2000);
        ReflectionTestUtils.setField(svc, "maxBboxDeg", 0.5);
        ReflectionTestUtils.setField(svc, "defaultCompetitorTags", "amenity=restaurant");
        ReflectionTestUtils.setField(svc, "demographicDensityCap", 20_000.0);
        ReflectionTestUtils.setField(svc, "demographicBlend", 0.0);
        ReflectionTestUtils.setField(svc, "cacheEnabled", true);
        ReflectionTestUtils.setField(svc, "cacheTtlSeconds", 3600);
    }
}
