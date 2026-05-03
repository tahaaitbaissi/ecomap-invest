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
import com.example.backend.entities.User;
import com.example.backend.repositories.DynamicProfileRepository;
import com.example.backend.scoring.HexScoringConfig;
import com.example.backend.scoring.HexagonRawScoringSupport;
import com.example.backend.scoring.TagWeight;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class SimulationServiceTest {

    @Mock
    private H3Core h3;

    @Mock
    private HexagonRawScoringSupport rawScoringSupport;

    @Mock
    private DynamicProfileRepository dynamicProfileRepository;

    @Mock
    private UserService userService;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    private SimulationService simulationService;

    private final UUID profileId = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
    private final UUID userId = UUID.fromString("11111111-2222-3333-4444-555555555555");

    @BeforeEach
    void setUp() {
        simulationService =
                new SimulationService(h3, rawScoringSupport, dynamicProfileRepository, userService, stringRedisTemplate);
        ReflectionTestUtils.setField(simulationService, "h3Resolution", 9);
        ReflectionTestUtils.setField(simulationService, "simulationTtlSeconds", 600);
    }

    @Test
    void simulateImpact_wrongOwner_forbidden() {
        User user = new User();
        user.setId(userId);
        when(userService.getUserByEmail("me@test.com")).thenReturn(user);
        DynamicProfile p = new DynamicProfile();
        p.setId(profileId);
        p.setUserId(UUID.randomUUID());
        when(dynamicProfileRepository.findById(profileId)).thenReturn(Optional.of(p));

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
                                "me@test.com"));
    }

    @Test
    void simulateImpact_driver_appliesRingWeights_andWritesRedis() {
        User user = new User();
        user.setId(userId);
        when(userService.getUserByEmail("me@test.com")).thenReturn(user);
        DynamicProfile p = new DynamicProfile();
        p.setId(profileId);
        p.setUserId(userId);
        when(dynamicProfileRepository.findById(profileId)).thenReturn(Optional.of(p));

        HexScoringConfig cfg =
                new HexScoringConfig(List.of(new TagWeight("amenity=cafe", 2.0)), List.of(), false);
        when(rawScoringSupport.buildConfigForProfile(profileId)).thenReturn(cfg);
        when(rawScoringSupport.findDriverWeight(cfg, "amenity=cafe")).thenReturn(Optional.of(2.0));
        when(rawScoringSupport.computeRaw(anyString(), eq(cfg))).thenReturn(100.0);
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

        org.mockito.Mockito.when(stringRedisTemplate.executePipelined(any(RedisCallback.class)))
                .thenReturn(List.of());

        SimulateResponse res =
                simulationService.simulateImpact(
                        33.5, -7.6, SimulationImpactType.DRIVER, "amenity=cafe", profileId, "sess-a", "me@test.com");

        assertEquals(2, res.affectedHexagons().size());
        assertEquals(100.0, res.affectedHexagons().get(0).score(), 0.001);
        assertEquals(0.0, res.affectedHexagons().get(1).score(), 0.001);

        verify(stringRedisTemplate).executePipelined(any(RedisCallback.class));
    }

    @Test
    void simulateImpact_competitor_deltaNegative() {
        User user = new User();
        user.setId(userId);
        when(userService.getUserByEmail("me@test.com")).thenReturn(user);
        DynamicProfile p = new DynamicProfile();
        p.setId(profileId);
        p.setUserId(userId);
        when(dynamicProfileRepository.findById(profileId)).thenReturn(Optional.of(p));

        HexScoringConfig cfg =
                new HexScoringConfig(List.of(), List.of(new TagWeight("amenity=bar", 1.0)), false);
        when(rawScoringSupport.buildConfigForProfile(profileId)).thenReturn(cfg);
        when(rawScoringSupport.findCompetitorWeight(cfg, "amenity=bar")).thenReturn(Optional.of(1.0));
        when(rawScoringSupport.computeRaw(anyString(), eq(cfg))).thenReturn(50.0);
        when(rawScoringSupport.boundaryPoints(anyString()))
                .thenReturn(List.of(new HexagonRawScoringSupport.LatLngRingPoint(0, 0)));

        long center = 1L;
        when(h3.latLngToCell(anyDouble(), anyDouble(), anyInt())).thenReturn(center);
        when(h3.gridDisk(center, 2)).thenReturn(List.of(center));
        when(h3.h3ToString(center)).thenReturn("only");
        when(h3.gridDistance(center, center)).thenReturn(0L);

        when(stringRedisTemplate.executePipelined(any(RedisCallback.class))).thenReturn(List.of());

        SimulateResponse res =
                simulationService.simulateImpact(
                        1, 2, SimulationImpactType.COMPETITOR, "amenity=bar", profileId, "s", "me@test.com");

        assertEquals(1, res.affectedHexagons().size());
        assertEquals(50.0, res.affectedHexagons().get(0).score(), 0.001);
    }
}
