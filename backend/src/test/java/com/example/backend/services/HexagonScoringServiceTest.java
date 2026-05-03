package com.example.backend.services;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.backend.controllers.dto.HexagonMapResponse;
import com.example.backend.scoring.HexScoringConfig;
import com.example.backend.scoring.HexagonRawScoringSupport;
import com.example.backend.scoring.TagWeight;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.uber.h3core.H3Core;
import com.uber.h3core.util.LatLng;
import java.util.List;
import java.util.NoSuchElementException;
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
        H3Core h3 = mock(H3Core.class);
        HexagonRawScoringSupport raw = mock(HexagonRawScoringSupport.class);
        HexagonScorePersistenceService persist = mock(HexagonScorePersistenceService.class);
        HexagonScoringService svc = newService(h3, raw, persist, null);
        wireDefaults(svc);

        assertThrows(IllegalArgumentException.class, () -> svc.getHexagonsInBbox(null, null));
        verify(h3, never()).polygonToCells(anyList(), anyList(), anyInt());
    }

    @Test
    void getHexagons_tinyBbox_grayMode_nullScore() {
        H3Core h3 = mock(H3Core.class);
        HexagonRawScoringSupport raw = mock(HexagonRawScoringSupport.class);
        long fakeCell = 0x891ea6c0d47ffffL;
        when(h3.polygonToCells(anyList(), anyList(), anyInt())).thenReturn(List.of(fakeCell));
        when(h3.h3ToString(fakeCell)).thenReturn("891ea6c0d47ffff");
        when(h3.stringToH3("891ea6c0d47ffff")).thenReturn(fakeCell);
        when(h3.cellToBoundary(fakeCell))
                .thenReturn(
                        List.of(
                                new LatLng(33.56, -7.60),
                                new LatLng(33.57, -7.58),
                                new LatLng(33.58, -7.59)));
        HexagonScorePersistenceService persist = mock(HexagonScorePersistenceService.class);
        HexagonScoringService svc = newService(h3, raw, persist, null);
        wireDefaults(svc);
        String bbox = "-7.59,33.57,-7.57,33.59";
        List<HexagonMapResponse> r = svc.getHexagonsInBbox(bbox, null);
        assertTrue(r.size() == 1);
        assertNull(r.get(0).score());
        verify(raw, never()).computeRaw(anyString(), any());
        verify(persist, never()).upsertViewportScores(any(), any());
    }

    @Test
    void getHexagons_unknownProfile_failsAfterViewportResolved() {
        H3Core h3 = mock(H3Core.class);
        HexagonRawScoringSupport raw = mock(HexagonRawScoringSupport.class);
        long fakeCell = 0x891ea6c0d47ffffL;
        when(h3.polygonToCells(anyList(), anyList(), anyInt())).thenReturn(List.of(fakeCell));
        when(h3.h3ToString(fakeCell)).thenReturn("891ea6c0d47ffff");
        UUID unknown = UUID.randomUUID();
        when(raw.buildConfigForProfile(unknown))
                .thenThrow(new NoSuchElementException("Unknown profile: " + unknown));
        HexagonScorePersistenceService persist = mock(HexagonScorePersistenceService.class);
        HexagonScoringService svc = newService(h3, raw, persist, null);
        wireDefaults(svc);
        String bbox = "-7.59,33.57,-7.57,33.59";
        assertThrows(NoSuchElementException.class, () -> svc.getHexagonsInBbox(bbox, unknown));
        verify(raw).buildConfigForProfile(unknown);
    }

    @Test
    void getHexagons_withProfile_parsesJson() {
        H3Core h3 = mock(H3Core.class);
        HexagonRawScoringSupport raw = mock(HexagonRawScoringSupport.class);
        long fakeCell = 0x891ea6c0d47ffffL;
        when(h3.polygonToCells(anyList(), anyList(), anyInt())).thenReturn(List.of(fakeCell));
        when(h3.h3ToString(fakeCell)).thenReturn("891ea6c0d47ffff");
        when(h3.stringToH3("891ea6c0d47ffff")).thenReturn(fakeCell);
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

        UUID profileId = UUID.randomUUID();
        HexScoringConfig cfg =
                new HexScoringConfig(
                        List.of(new TagWeight("category=school", 1.0)),
                        List.of(new TagWeight("amenity=cafe", 0.5)),
                        false);
        when(raw.buildConfigForProfile(profileId)).thenReturn(cfg);
        when(raw.computeRaw(eq("891ea6c0d47ffff"), eq(cfg))).thenReturn(0.0);

        HexagonScorePersistenceService persist = mock(HexagonScorePersistenceService.class);
        HexagonScoringService svc = newService(h3, raw, persist, null);
        wireDefaults(svc);
        String bbox = "-7.59,33.57,-7.57,33.59";
        List<HexagonMapResponse> r = svc.getHexagonsInBbox(bbox, profileId);
        assertTrue(!r.isEmpty());
        verify(persist).upsertViewportScores(eq(profileId), anyMap());
    }

    @Test
    void getHexagons_cacheHit_skipsDbCounts() {
        H3Core h3 = mock(H3Core.class);
        HexagonRawScoringSupport raw = mock(HexagonRawScoringSupport.class);
        long fakeCell = 0x891ea6c0d47ffffL;
        when(h3.polygonToCells(anyList(), anyList(), anyInt())).thenReturn(List.of(fakeCell));
        when(h3.h3ToString(fakeCell)).thenReturn("891ea6c0d47ffff");
        when(h3.stringToH3("891ea6c0d47ffff")).thenReturn(fakeCell);
        when(h3.cellToBoundary(fakeCell))
                .thenReturn(
                        List.of(
                                new LatLng(33.56, -7.60),
                                new LatLng(33.57, -7.58),
                                new LatLng(33.58, -7.59)));

        UUID profileId = UUID.randomUUID();
        HexScoringConfig cfg = new HexScoringConfig(List.of(), List.of(), false);
        when(raw.buildConfigForProfile(profileId)).thenReturn(cfg);

        @SuppressWarnings("unchecked")
        ValueOperations<String, String> vops = mock(ValueOperations.class);
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        when(redis.opsForValue()).thenReturn(vops);
        when(vops.multiGet(anyList())).thenReturn(List.of("0.3"));

        HexagonScorePersistenceService persist = mock(HexagonScorePersistenceService.class);
        HexagonScoringService svc = newService(h3, raw, persist, redis);
        wireDefaults(svc);
        String bbox = "-7.59,33.57,-7.57,33.59";
        List<HexagonMapResponse> r = svc.getHexagonsInBbox(bbox, profileId);
        assertTrue(r.size() == 1);
        verify(raw, never()).computeRaw(anyString(), any());
    }

    private static HexagonScoringService newService(
            H3Core h3, HexagonRawScoringSupport raw, HexagonScorePersistenceService persist, StringRedisTemplate redis) {
        return new HexagonScoringService(h3, raw, persist, redis);
    }

    private static void wireDefaults(HexagonScoringService svc) {
        ReflectionTestUtils.setField(svc, "h3Resolution", 9);
        ReflectionTestUtils.setField(svc, "maxCells", 2000);
        ReflectionTestUtils.setField(svc, "maxBboxDeg", 0.5);
        ReflectionTestUtils.setField(svc, "cacheEnabled", true);
        ReflectionTestUtils.setField(svc, "cacheTtlSeconds", 3600);
    }
}
