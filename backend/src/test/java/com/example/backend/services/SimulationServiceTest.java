package com.example.backend.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.backend.controllers.dto.HexagonMapResponse;
import com.example.backend.controllers.dto.SimulationImpactType;
import com.example.backend.controllers.dto.SimulateResponse;
import com.example.backend.entities.DynamicProfile;
import com.example.backend.repositories.H3HexagonRepository;
import com.example.backend.scoring.HexScoringConfig;
import com.example.backend.scoring.HexagonRawScoringSupport;
import com.example.backend.scoring.RawScoreRefBounds;
import com.example.backend.services.ProfileScoreScaleService;
import com.example.backend.scoring.TagWeight;
import com.example.backend.services.profile.DynamicProfileService;
import com.example.backend.services.profile.ProfileTagCatalog;
import com.uber.h3core.H3Core;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class SimulationServiceTest {

    @Mock
    private H3Core h3;

    @Mock
    private HexagonRawScoringSupport rawScoringSupport;

    @Mock
    private DynamicProfileService dynamicProfileService;

    @Mock
    private ProfileTagCatalog profileTagCatalog;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private H3HexagonRepository h3HexagonRepository;

    @Mock
    private ProfileScoreScaleService profileScoreScaleService;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private SimulationService simulationService;

    private final UUID profileId = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
    private final UUID userId = UUID.fromString("11111111-2222-3333-4444-555555555555");

    @BeforeEach
    void setUp() {
        simulationService =
                new SimulationService(
                        h3,
                        rawScoringSupport,
                        dynamicProfileService,
                        profileTagCatalog,
                        stringRedisTemplate,
                        h3HexagonRepository,
                        profileScoreScaleService);
        ReflectionTestUtils.setField(simulationService, "h3Resolution", 9);
        ReflectionTestUtils.setField(simulationService, "simulationTtlSeconds", 600);
        ReflectionTestUtils.setField(simulationService, "maxBboxDeg", 0.5);
        org.mockito.Mockito.lenient()
                .when(profileTagCatalog.canonicalTag(anyString()))
                .thenAnswer(inv -> Optional.of(inv.getArgument(0, String.class).trim()));
        org.mockito.Mockito.lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        org.mockito.Mockito.lenient().when(valueOperations.multiGet(any())).thenReturn(List.of());
    }

    @Test
    void simulateImpact_wrongOwner_forbidden() {
        when(dynamicProfileService.getOwnedActiveEntity("me@test.com", profileId))
                .thenThrow(new AccessDeniedException("You do not own this profile"));

        assertThrows(
                AccessDeniedException.class,
                () ->
                        simulationService.simulateImpact(
                                33.5,
                                -7.6,
                                SimulationImpactType.DRIVER,
                                "amenity=cafe",
                                profileId,
                                "sess-1",
                                "-7.7,33.4,-7.5,33.6",
                                "me@test.com"));
    }

    @Test
    void simulateImpact_driver_appliesRingWeights_andWritesRedis() {
        DynamicProfile p = new DynamicProfile();
        p.setId(profileId);
        p.setUserId(userId);
        when(dynamicProfileService.getOwnedActiveEntity("me@test.com", profileId)).thenReturn(p);

        HexScoringConfig cfg =
                new HexScoringConfig(List.of(new TagWeight("amenity=cafe", 2.0)), List.of(), false);
        when(rawScoringSupport.buildConfigForProfile(profileId)).thenReturn(cfg);
        when(rawScoringSupport.findDriverWeight(cfg, "amenity=cafe")).thenReturn(Optional.of(2.0));
        when(rawScoringSupport.boundaryPoints(anyString()))
                .thenReturn(List.of(new HexagonRawScoringSupport.LatLngRingPoint(33.0, -7.0)));

        long center = 123L;
        long c1 = 123L;
        long c2 = 124L;
        when(h3.latLngToCell(33.5, -7.6, 9)).thenReturn(center);
        when(h3.gridDisk(center, 2)).thenReturn(List.of(c1, c2));
        when(h3.h3ToString(c1)).thenReturn("h1");
        when(h3.h3ToString(c2)).thenReturn("h2");
        when(h3.gridDistance(center, c1)).thenReturn(0L);
        when(h3.gridDistance(center, c2)).thenReturn(1L);
        when(h3HexagonRepository.findH3IndicesIntersectingBbox(-7.7, 33.4, -7.5, 33.6))
                .thenReturn(List.of("h1", "h2", "h3"));
        when(rawScoringSupport.computeRaw("h1", cfg)).thenReturn(100.0);
        when(rawScoringSupport.computeRaw("h2", cfg)).thenReturn(100.0);
        when(rawScoringSupport.computeRaw("h3", cfg)).thenReturn(95.0);
        when(profileScoreScaleService.resolveRefBounds(profileId, cfg))
                .thenReturn(new RawScoreRefBounds(95.0, 100.0));

        org.mockito.Mockito.when(stringRedisTemplate.executePipelined(any(RedisCallback.class)))
                .thenReturn(List.of());

        SimulateResponse res =
                simulationService.simulateImpact(
                        33.5,
                        -7.6,
                        SimulationImpactType.DRIVER,
                        "amenity=cafe",
                        profileId,
                        "sess-a",
                        "-7.7,33.4,-7.5,33.6",
                        "me@test.com");

        assertEquals(3, res.affectedHexagons().size());
        assertEquals(100.0, res.affectedHexagons().get(0).score(), 0.001);
        assertEquals(100.0, res.affectedHexagons().get(1).score(), 0.001);
        assertEquals(0.0, res.affectedHexagons().get(2).score(), 0.001);

        verify(stringRedisTemplate, org.mockito.Mockito.times(2)).executePipelined(any(RedisCallback.class));
    }

    @Test
    void simulateImpact_competitor_deltaNegative() {
        DynamicProfile p = new DynamicProfile();
        p.setId(profileId);
        p.setUserId(userId);
        when(dynamicProfileService.getOwnedActiveEntity("me@test.com", profileId)).thenReturn(p);

        HexScoringConfig cfg =
                new HexScoringConfig(List.of(), List.of(new TagWeight("amenity=bar", 1.0)), false);
        when(rawScoringSupport.buildConfigForProfile(profileId)).thenReturn(cfg);
        when(rawScoringSupport.findCompetitorWeight(cfg, "amenity=bar")).thenReturn(Optional.of(1.0));
        when(rawScoringSupport.boundaryPoints(anyString()))
                .thenReturn(List.of(new HexagonRawScoringSupport.LatLngRingPoint(0, 0)));

        long center = 1L;
        when(h3.latLngToCell(anyDouble(), anyDouble(), anyInt())).thenReturn(center);
        when(h3.gridDisk(center, 2)).thenReturn(List.of(center));
        when(h3.h3ToString(center)).thenReturn("only");
        when(h3.gridDistance(center, center)).thenReturn(0L);
        when(h3HexagonRepository.findH3IndicesIntersectingBbox(-7.7, 33.4, -7.5, 33.6))
                .thenReturn(List.of("only", "other"));
        when(rawScoringSupport.computeRaw("only", cfg)).thenReturn(50.0);
        when(rawScoringSupport.computeRaw("other", cfg)).thenReturn(55.0);
        when(profileScoreScaleService.resolveRefBounds(profileId, cfg))
                .thenReturn(new RawScoreRefBounds(50.0, 55.0));

        when(stringRedisTemplate.executePipelined(any(RedisCallback.class))).thenReturn(List.of());

        SimulateResponse res =
                simulationService.simulateImpact(
                        1,
                        2,
                        SimulationImpactType.COMPETITOR,
                        "amenity=bar",
                        profileId,
                        "s",
                        "-7.7,33.4,-7.5,33.6",
                        "me@test.com");

        assertEquals(2, res.affectedHexagons().size());
        assertEquals(0.0, res.affectedHexagons().get(0).score(), 0.001);
        assertEquals(100.0, res.affectedHexagons().get(1).score(), 0.001);
    }
}
