package com.example.backend.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.backend.controllers.dto.HexagonMapResponse;
import com.example.backend.controllers.dto.OpportunitySimulateRequest;
import com.example.backend.controllers.dto.OpportunitySimulateResponse;
import com.example.backend.controllers.dto.SimulateRequest;
import com.example.backend.controllers.dto.SimulateResponse;
import com.example.backend.controllers.dto.SimulationImpactType;
import com.example.backend.entities.DynamicProfile;
import com.example.backend.scoring.HexagonRawScoringSupport;
import com.example.backend.services.SimulationService;
import com.example.backend.services.opportunity.OpportunityScoreService;
import com.example.backend.services.profile.DynamicProfileService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

@ExtendWith(MockitoExtension.class)
class SimulationControllerTest {

    @Mock
    private SimulationService simulationService;

    @Mock
    private OpportunityScoreService opportunityScoreService;

    @Mock
    private HexagonRawScoringSupport hexagonRawScoringSupport;

    @Mock
    private DynamicProfileService dynamicProfileService;

    private SimulationController simulationController;

    @BeforeEach
    void initController() {
        simulationController =
                new SimulationController(
                        simulationService,
                        opportunityScoreService,
                        hexagonRawScoringSupport,
                        dynamicProfileService);
    }

    private static UsernamePasswordAuthenticationToken auth() {
        return new UsernamePasswordAuthenticationToken("u@test.com", "x", Collections.emptyList());
    }

    @Test
    void post_simulate_ok() {
        SimulateResponse body =
                new SimulateResponse(
                        List.of(
                                new HexagonMapResponse(
                                        "891ea6c0d47ffff",
                                        77.0,
                                        List.of(new HexagonMapResponse.LatLng(33.0, -7.0)))));
        when(simulationService.simulateImpact(
                        anyDouble(),
                        anyDouble(),
                        eq(SimulationImpactType.DRIVER),
                        eq("amenity=cafe"),
                        any(UUID.class),
                        eq("sess-1"),
                        eq("-7.7,33.4,-7.5,33.6"),
                        eq("u@test.com")))
                .thenReturn(body);

        SimulateRequest req =
                new SimulateRequest(
                        33.5,
                        -7.6,
                        SimulationImpactType.DRIVER,
                        "amenity=cafe",
                        UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"),
                        "sess-1",
                        "-7.7,33.4,-7.5,33.6");

        ResponseEntity<?> res = simulationController.simulate(auth(), req);
        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertEquals(body, res.getBody());
    }

    @Test
    void post_opportunity_ok() {
        UUID pid = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
        DynamicProfile dp = new DynamicProfile();
        dp.setId(pid);
        dp.setName("Avocat test");
        dp.setUserQuery("avocat");
        var cfg = new com.example.backend.scoring.HexScoringConfig(List.of(), List.of(), false);
        OpportunitySimulateResponse resp =
                new OpportunitySimulateResponse(
                        72.0,
                        60,
                        6,
                        10,
                        12,
                        "891ea6c0d47ffff",
                        2L,
                        1L,
                        "lawyer",
                        "",
                        Map.of());

        when(dynamicProfileService.getOwnedActiveEntity("u@test.com", pid)).thenReturn(dp);
        when(hexagonRawScoringSupport.buildConfigForProfile(pid)).thenReturn(cfg);
        when(opportunityScoreService.compute(dp, 33.5, -7.6, cfg, false)).thenReturn(resp);

        OpportunitySimulateRequest req = new OpportunitySimulateRequest(33.5, -7.6, pid, false);
        ResponseEntity<?> entity = simulationController.opportunity(auth(), req);
        assertEquals(HttpStatus.OK, entity.getStatusCode());
        assertEquals(resp, entity.getBody());
    }

    @Test
    void post_simulate_forbidden() {
        when(simulationService.simulateImpact(
                        anyDouble(),
                        anyDouble(),
                        any(SimulationImpactType.class),
                        anyString(),
                        any(UUID.class),
                        anyString(),
                        anyString(),
                        anyString()))
                .thenThrow(new AccessDeniedException("You do not own this profile"));

        SimulateRequest req =
                new SimulateRequest(
                        33.5,
                        -7.6,
                        SimulationImpactType.DRIVER,
                        "x",
                        UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"),
                        "sess-1",
                        "-7.7,33.4,-7.5,33.6");

        ResponseEntity<?> res = simulationController.simulate(auth(), req);
        assertEquals(HttpStatus.FORBIDDEN, res.getStatusCode());
    }

    @Test
    void delete_session_noContent() {
        ResponseEntity<?> res = simulationController.clearSession("my-session");
        assertEquals(HttpStatus.NO_CONTENT, res.getStatusCode());
        verify(simulationService).deleteSimulationSession("my-session");
    }

    @Test
    void simulateRequest_jsonRoundTrip() throws Exception {
        ObjectMapper om = new ObjectMapper();
        SimulateRequest req =
                new SimulateRequest(
                        33.5,
                        -7.6,
                        SimulationImpactType.COMPETITOR,
                        "amenity=restaurant",
                        UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"),
                        "sess-9",
                        "-7.7,33.4,-7.5,33.6");
        String json = om.writeValueAsString(req);
        SimulateRequest back = om.readValue(json, SimulateRequest.class);
        assertEquals(req.profileId(), back.profileId());
        assertEquals(SimulationImpactType.COMPETITOR, back.type());
    }
}
