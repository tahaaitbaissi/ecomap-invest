package com.example.backend.services.opportunity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.example.backend.entities.DynamicProfile;
import com.example.backend.foottraffic.config.FootTrafficProperties;
import com.example.backend.foottraffic.services.FootTrafficService;
import com.example.backend.scoring.HexScoringConfig;
import com.example.backend.scoring.HexagonRawScoringSupport;
import com.example.backend.scoring.TagWeight;
import com.uber.h3core.H3Core;
import java.util.List;
import java.util.OptionalDouble;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class OpportunityScoreServiceTest {

    @Mock
    private HexagonRawScoringSupport rawScoringSupport;

    @Mock
    private FootTrafficService footTrafficService;

    @Mock
    private FootTrafficProperties footTrafficProperties;

    @Mock
    private BusinessFitCatalog businessFitCatalog;

    @Mock
    private OpportunityExplanationService opportunityExplanationService;

    @Mock
    private H3Core h3;

    @InjectMocks
    private OpportunityScoreService opportunityScoreService;

    private static final String HEX = "8923c3b2b9bffff";

    @BeforeEach
    void wireConfig() {
        ReflectionTestUtils.setField(opportunityScoreService, "demandRadiusMeters", 800.0);
        ReflectionTestUtils.setField(opportunityScoreService, "competitorRadiusMeters", 500.0);
        ReflectionTestUtils.setField(opportunityScoreService, "fitRadiusMeters", 600.0);
        ReflectionTestUtils.setField(opportunityScoreService, "demandScale", 8.0);
        ReflectionTestUtils.setField(opportunityScoreService, "h3Resolution", 9);
        when(h3.latLngToCell(anyDouble(), anyDouble(), eq(9))).thenReturn(999L);
        when(h3.h3ToString(999L)).thenReturn(HEX);
        when(businessFitCatalog.matchArchetype(any(), any()))
                .thenReturn(
                        new BusinessFitCatalog.ResolvedArchetype(
                                "lawyer", Pattern.compile(".*"), List.of(), 2.0, 0.0));
        when(footTrafficService.getPeakHourlyNorm(any())).thenReturn(OptionalDouble.empty());
        lenient().when(footTrafficProperties.getBlendAlpha()).thenReturn(0.5);
        lenient().when(footTrafficProperties.getTermWeight()).thenReturn(0.25);
    }

    @Test
    void demand_uses_hex_cell_drivers_when_radius_from_click_has_none() {
        DynamicProfile profile = new DynamicProfile();
        profile.setName("Avocat");
        profile.setUserQuery("cabinet");

        HexScoringConfig cfg = new HexScoringConfig(List.of(new TagWeight("office", 1.0)), List.of(), false);

        when(rawScoringSupport.computeParts(eq(HEX), eq(cfg)))
                .thenReturn(new HexagonRawScoringSupport.HexRawParts(40.0, 0.0, 0.0, 0.0, 0.0));
        when(rawScoringSupport.weightedDriversNearLatLng(anyDouble(), anyDouble(), eq(800.0), eq(cfg)))
                .thenReturn(0.0);
        when(rawScoringSupport.competitorCountNearLatLng(anyDouble(), anyDouble(), eq(500.0), eq(cfg)))
                .thenReturn(0L);
        when(rawScoringSupport.competitorCountWithinHex(eq(HEX), eq(cfg))).thenReturn(0L);

        var out = opportunityScoreService.compute(profile, 33.57, -7.59, cfg, false);

        assertTrue(out.demandScore() >= 29.0, "demand should reflect in-hex drivers, not collapse to zero");
        assertEquals(HEX, out.h3Index());
        assertEquals(0L, out.competitorCountNearby());
        assertEquals(0L, out.competitorCountInHex());
        assertEquals(0.0, out.competitionPenalty());
    }

    @Test
    void demand_includes_foot_traffic_even_when_demographics_disabled() {
        DynamicProfile profile = new DynamicProfile();
        profile.setName("Avocat");
        profile.setUserQuery("cabinet");

        // demographics disabled
        HexScoringConfig cfg = new HexScoringConfig(List.of(), List.of(), false);

        when(rawScoringSupport.computeParts(eq(HEX), eq(cfg)))
                .thenReturn(new HexagonRawScoringSupport.HexRawParts(0.0, 0.0, 0.0, 0.0, 0.0));
        when(rawScoringSupport.weightedDriversNearLatLng(anyDouble(), anyDouble(), eq(800.0), eq(cfg)))
                .thenReturn(0.0);
        when(rawScoringSupport.competitorCountNearLatLng(anyDouble(), anyDouble(), eq(500.0), eq(cfg)))
                .thenReturn(0L);
        when(rawScoringSupport.competitorCountWithinHex(eq(HEX), eq(cfg))).thenReturn(0L);

        when(footTrafficService.getPeakHourlyNorm(eq(HEX))).thenReturn(OptionalDouble.of(0.8));
        when(footTrafficProperties.getBlendAlpha()).thenReturn(0.25);

        var out = opportunityScoreService.compute(profile, 33.57, -7.59, cfg, false);

        // pNorm=0, demandNorm = trafficNorm*(1-alpha) = 0.8*0.75 = 0.6
        assertEquals(0.6, out.metrics().get("demandNorm"), 1e-9);
        assertTrue(out.demandScore() > 0.0, "foot traffic should contribute to demandScore");
    }

    @Test
    void competition_saturation_uses_max_of_nearby_radius_and_hex_cell_counts() {
        DynamicProfile profile = new DynamicProfile();
        profile.setName("Avocat");

        HexScoringConfig cfg = new HexScoringConfig(List.of(), List.of(), false);

        when(rawScoringSupport.computeParts(eq(HEX), eq(cfg)))
                .thenReturn(new HexagonRawScoringSupport.HexRawParts(10.0, 0.0, 0.0, 0.0, 3.0));
        when(rawScoringSupport.weightedDriversNearLatLng(anyDouble(), anyDouble(), eq(800.0), eq(cfg)))
                .thenReturn(0.0);
        when(rawScoringSupport.competitorCountNearLatLng(anyDouble(), anyDouble(), eq(500.0), eq(cfg)))
                .thenReturn(0L);
        when(rawScoringSupport.competitorCountWithinHex(eq(HEX), eq(cfg))).thenReturn(4L);

        var out = opportunityScoreService.compute(profile, 33.0, -7.0, cfg, false);

        assertEquals(12.0, out.competitionPenalty());
        assertEquals(0.0, out.clusterEffectBonus());
    }
}
