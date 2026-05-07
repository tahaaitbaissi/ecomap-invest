package com.example.backend.controllers;

import com.example.backend.controllers.dto.OpportunitySimulateRequest;
import com.example.backend.controllers.dto.OpportunitySimulateResponse;
import com.example.backend.controllers.dto.SimulateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.example.backend.controllers.dto.SimulateResponse;
import com.example.backend.services.SimulationService;
import com.example.backend.services.opportunity.OpportunityScoreService;
import com.example.backend.scoring.HexagonRawScoringSupport;
import com.example.backend.services.profile.DynamicProfileService;
import jakarta.validation.Valid;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Simulation", description = "What-if hex score deltas (Redis-backed)")
@RestController
@RequestMapping("/api/v1/simulate")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class SimulationController {

    private final SimulationService simulationService;
    private final OpportunityScoreService opportunityScoreService;
    private final HexagonRawScoringSupport hexagonRawScoringSupport;
    private final DynamicProfileService dynamicProfileService;

    @Operation(summary = "Apply a hypothetical driver/competitor impact")
    @PostMapping
    public ResponseEntity<?> simulate(Authentication authentication, @Valid @RequestBody SimulateRequest request) {
        try {
            SimulateResponse out =
                    simulationService.simulateImpact(
                            request.lat(),
                            request.lng(),
                            request.type(),
                            request.tag().trim(),
                            request.profileId(),
                            request.sessionId().trim(),
                            request.bbox().trim(),
                            authentication.getName());
            return ResponseEntity.ok(out);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        } catch (NoSuchElementException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
        } catch (AccessDeniedException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ex.getMessage());
        }
    }

    @Operation(summary = "Business-specific opportunity at a location for the selected profile")
    @PostMapping("/opportunity")
    public ResponseEntity<?> opportunity(
            Authentication authentication, @Valid @RequestBody OpportunitySimulateRequest request) {
        try {
            var profile = dynamicProfileService.getOwnedActiveEntity(authentication.getName(), request.profileId());
            var cfg = hexagonRawScoringSupport.buildConfigForProfile(request.profileId());
            OpportunitySimulateResponse out =
                    opportunityScoreService.compute(
                            profile, request.lat(), request.lng(), cfg, request.explain());
            return ResponseEntity.ok(out);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        } catch (NoSuchElementException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
        } catch (AccessDeniedException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ex.getMessage());
        }
    }

    @Operation(summary = "Clear Redis keys for a simulation session")
    @DeleteMapping("/{sessionId}")
    public ResponseEntity<?> clearSession(@PathVariable("sessionId") String sessionId) {
        try {
            simulationService.deleteSimulationSession(sessionId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }
}
