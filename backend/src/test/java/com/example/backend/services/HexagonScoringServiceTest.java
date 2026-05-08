package com.example.backend.services;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.backend.controllers.dto.HexagonMapResponse;
import com.example.backend.repositories.H3HexagonRepository;
import com.example.backend.scoring.HexScoringConfig;
import com.example.backend.scoring.HexagonRawScoringSupport;
import com.example.backend.scoring.RawScoreRefBounds;
import com.example.backend.scoring.TagWeight;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.uber.h3core.H3Core;
import com.uber.h3core.util.LatLng;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

class HexagonScoringServiceTest {

    private static final RawScoreRefBounds SCALE_UNUSED = new RawScoreRefBounds(0, 100);

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
    void minMaxOrNeutral_ignoresNullAndNonFinite() {
        assertArrayEquals(
                new double[] {1, 3, 0.0},
                HexagonScoringService.minMaxOrNeutral(
                        Arrays.asList(null, Double.NaN, Double.POSITIVE_INFINITY, 1.0, 3.0)),
                0.001);
    }

    @Test
    void getHexagons_h3ResolutionOutOfRange_throws() {
        H3Core h3 = mock(H3Core.class);
        HexagonRawScoringSupport raw = mock(HexagonRawScoringSupport.class);
        HexagonScorePersistenceService persist = mock(HexagonScorePersistenceService.class);
        H3HexagonRepository hexRepo = mock(H3HexagonRepository.class);
        HexagonScoringService svc = newService(h3, raw, persist, null, hexRepo, SCALE_UNUSED);
        wireDefaults(svc);

        assertThrows(
                IllegalArgumentException.class,
                () -> svc.getHexagonsInBbox("-7.59,33.57,-7.57,33.59", null, 6));
        verify(hexRepo, never())
                .findH3IndicesIntersectingBbox(
                        anyDouble(), anyDouble(), anyDouble(), anyDouble());
    }

    @Test
    void parseBbox_inheritedViaGetHexagons_missingArg() {
        H3Core h3 = mock(H3Core.class);
        HexagonRawScoringSupport raw = mock(HexagonRawScoringSupport.class);
        HexagonScorePersistenceService persist = mock(HexagonScorePersistenceService.class);
        H3HexagonRepository hexRepo = mock(H3HexagonRepository.class);
        HexagonScoringService svc = newService(h3, raw, persist, null, hexRepo, SCALE_UNUSED);
        wireDefaults(svc);

        assertThrows(IllegalArgumentException.class, () -> svc.getHexagonsInBbox(null, null));
        verify(hexRepo, never())
                .findH3IndicesIntersectingBbox(
                        anyDouble(), anyDouble(), anyDouble(), anyDouble());
    }

    @Test
    void getHexagons_tinyBbox_grayMode_nullScore() {
        H3Core h3 = mock(H3Core.class);
        HexagonRawScoringSupport raw = mock(HexagonRawScoringSupport.class);
        long fakeCell = 0x891ea6c0d47ffffL;
        H3HexagonRepository hexRepo = mock(H3HexagonRepository.class);
        when(hexRepo.findH3IndicesIntersectingBbox(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(List.of("891ea6c0d47ffff"));
        when(h3.stringToH3("891ea6c0d47ffff")).thenReturn(fakeCell);
        when(h3.cellToBoundary(fakeCell))
                .thenReturn(
                        List.of(
                                new LatLng(33.56, -7.60),
                                new LatLng(33.57, -7.58),
                                new LatLng(33.58, -7.59)));
        HexagonScorePersistenceService persist = mock(HexagonScorePersistenceService.class);
        HexagonScoringService svc = newService(h3, raw, persist, null, hexRepo, SCALE_UNUSED);
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
        H3HexagonRepository hexRepo = mock(H3HexagonRepository.class);
        when(hexRepo.findH3IndicesIntersectingBbox(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(List.of("891ea6c0d47ffff"));
        UUID unknown = UUID.randomUUID();
        when(raw.buildConfigForProfile(unknown))
                .thenThrow(new NoSuchElementException("Unknown profile: " + unknown));
        HexagonScorePersistenceService persist = mock(HexagonScorePersistenceService.class);
        HexagonScoringService svc = newService(h3, raw, persist, null, hexRepo, SCALE_UNUSED);
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
        H3HexagonRepository hexRepo = mock(H3HexagonRepository.class);
        when(hexRepo.findH3IndicesIntersectingBbox(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(List.of("891ea6c0d47ffff"));
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
        HexagonScoringService svc = newService(h3, raw, persist, null, hexRepo, RawScoreRefBounds.degenerate(0));
        wireDefaults(svc);
        String bbox = "-7.59,33.57,-7.57,33.59";
        List<HexagonMapResponse> r = svc.getHexagonsInBbox(bbox, profileId);
        assertTrue(!r.isEmpty());
        assertEquals(50.0, r.get(0).score(), 0.001);
        verify(persist).upsertViewportScores(eq(profileId), argThat(m -> m.size() == 1 && Math.abs(m.get("891ea6c0d47ffff") - 50.0) < 0.001));
    }

    @Test
    void getHexagons_withProfile_uniformRawAcross_manyCells_returnsNullScores() {
        H3Core h3 = mock(H3Core.class);
        HexagonRawScoringSupport raw = mock(HexagonRawScoringSupport.class);
        H3HexagonRepository hexRepo = mock(H3HexagonRepository.class);
        when(hexRepo.findH3IndicesIntersectingBbox(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(List.of("hexA", "hexB"));
        UUID profileId = UUID.randomUUID();

        when(h3.stringToH3("hexA")).thenReturn(101L);
        when(h3.stringToH3("hexB")).thenReturn(102L);
        when(h3.cellToBoundary(101L))
                .thenReturn(
                        List.of(
                                new LatLng(33.56, -7.60),
                                new LatLng(33.57, -7.58),
                                new LatLng(33.58, -7.59)));
        when(h3.cellToBoundary(102L))
                .thenReturn(
                        List.of(
                                new LatLng(33.50, -7.61),
                                new LatLng(33.51, -7.59),
                                new LatLng(33.52, -7.60)));

        HexScoringConfig cfg =
                new HexScoringConfig(
                        List.of(new TagWeight("office=company", 1.0)),
                        List.of(new TagWeight("amenity=bank", 0.5)),
                        false);
        when(raw.buildConfigForProfile(profileId)).thenReturn(cfg);
        when(raw.computeRaw(eq("hexA"), eq(cfg))).thenReturn(0.0);
        when(raw.computeRaw(eq("hexB"), eq(cfg))).thenReturn(0.0);

        HexagonScorePersistenceService persist = mock(HexagonScorePersistenceService.class);
        HexagonScoringService svc = newService(h3, raw, persist, null, hexRepo, RawScoreRefBounds.degenerate(0));
        wireDefaults(svc);
        String bbox = "-7.59,33.57,-7.57,33.59";
        List<HexagonMapResponse> r = svc.getHexagonsInBbox(bbox, profileId);
        assertEquals(2, r.size());
        assertNull(r.get(0).score());
        assertNull(r.get(1).score());
        verify(persist).upsertViewportScores(eq(profileId), argThat(Map::isEmpty));
    }

    @Test
    void getHexagons_aggregatedResolution8_averagesChildRawsAndSkipsUpsert() {
        H3Core h3 = mock(H3Core.class);
        HexagonRawScoringSupport raw = mock(HexagonRawScoringSupport.class);
        H3HexagonRepository hexRepo = mock(H3HexagonRepository.class);
        when(hexRepo.findH3IndicesIntersectingBbox(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(List.of("c1", "c2", "c3"));

        when(h3.stringToH3("c1")).thenReturn(101L);
        when(h3.stringToH3("c2")).thenReturn(102L);
        when(h3.stringToH3("c3")).thenReturn(103L);
        when(h3.cellToParent(101L, 8)).thenReturn(201L);
        when(h3.cellToParent(102L, 8)).thenReturn(201L);
        when(h3.cellToParent(103L, 8)).thenReturn(202L);
        when(h3.h3ToString(201L)).thenReturn("pA");
        when(h3.h3ToString(202L)).thenReturn("pB");

        when(h3.stringToH3("pA")).thenReturn(201L);
        when(h3.stringToH3("pB")).thenReturn(202L);
        when(h3.cellToBoundary(201L))
                .thenReturn(List.of(new LatLng(33.0, -7.0), new LatLng(33.01, -7.0), new LatLng(33.0, -7.01)));
        when(h3.cellToBoundary(202L))
                .thenReturn(List.of(new LatLng(33.1, -7.1), new LatLng(33.11, -7.1), new LatLng(33.1, -7.11)));

        UUID profileId = UUID.randomUUID();
        HexScoringConfig cfg = new HexScoringConfig(List.of(), List.of(), false);
        when(raw.buildConfigForProfile(profileId)).thenReturn(cfg);
        when(raw.computeRaw(eq("c1"), eq(cfg))).thenReturn(10.0);
        when(raw.computeRaw(eq("c2"), eq(cfg))).thenReturn(30.0);
        when(raw.computeRaw(eq("c3"), eq(cfg))).thenReturn(40.0);

        HexagonScorePersistenceService persist = mock(HexagonScorePersistenceService.class);
        HexagonScoringService svc = newService(h3, raw, persist, null, hexRepo, new RawScoreRefBounds(10, 50));
        wireDefaults(svc);

        String bbox = "-7.59,33.57,-7.57,33.59";
        List<HexagonMapResponse> r = svc.getHexagonsInBbox(bbox, profileId, 8);
        assertEquals(2, r.size());
        assertEquals("pA", r.get(0).h3Index());
        assertEquals(25.0, r.get(0).score(), 0.001);
        assertEquals("pB", r.get(1).h3Index());
        assertEquals(75.0, r.get(1).score(), 0.001);
        verify(persist, never()).upsertViewportScores(any(), any());
    }

    @Test
    void getHexagons_cacheHit_skipsDbCounts() {
        H3Core h3 = mock(H3Core.class);
        HexagonRawScoringSupport raw = mock(HexagonRawScoringSupport.class);
        long fakeCell = 0x891ea6c0d47ffffL;
        H3HexagonRepository hexRepo = mock(H3HexagonRepository.class);
        when(hexRepo.findH3IndicesIntersectingBbox(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(List.of("891ea6c0d47ffff"));
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
        HexagonScoringService svc = newService(h3, raw, persist, redis, hexRepo, new RawScoreRefBounds(0, 1));
        wireDefaults(svc);
        String bbox = "-7.59,33.57,-7.57,33.59";
        List<HexagonMapResponse> r = svc.getHexagonsInBbox(bbox, profileId);
        assertTrue(r.size() == 1);
        assertEquals(30.0, r.get(0).score(), 0.001);
        verify(raw, never()).computeRaw(anyString(), any());
        verify(vops)
                .multiGet(argThat(keys -> keys.size() == 1
                        && keys.iterator().next().startsWith("score:v3:p1d1t0:" + profileId + ":")));
    }

    private static HexagonScoringService newService(
            H3Core h3,
            HexagonRawScoringSupport raw,
            HexagonScorePersistenceService persist,
            StringRedisTemplate redis,
            H3HexagonRepository h3HexagonRepository,
            RawScoreRefBounds refStub) {
        ProfileScoreScaleService scale = mock(ProfileScoreScaleService.class);
        Mockito.lenient().when(scale.resolveRefBounds(any(UUID.class), any())).thenReturn(refStub);
        var versions = mock(com.example.backend.services.admin.ScoreCacheVersionService.class);
        Mockito.lenient().when(versions.getPoiVersion()).thenReturn(1L);
        Mockito.lenient().when(versions.getDemoVersion()).thenReturn(1L);
        Mockito.lenient().when(versions.getTrafficVersion()).thenReturn(0L);
        return new HexagonScoringService(h3, raw, persist, h3HexagonRepository, redis, scale, versions);
    }

    private static void wireDefaults(HexagonScoringService svc) {
        ReflectionTestUtils.setField(svc, "maxCells", 2000);
        ReflectionTestUtils.setField(svc, "maxBboxDeg", 0.5);
        ReflectionTestUtils.setField(svc, "cacheEnabled", true);
        ReflectionTestUtils.setField(svc, "cacheTtlSeconds", 3600);
    }
}
