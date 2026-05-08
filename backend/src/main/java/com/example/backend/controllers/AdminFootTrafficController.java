package com.example.backend.controllers;

import com.example.backend.audit.Audited;
import com.example.backend.controllers.dto.foottraffic.AdminFootTrafficParamsUpsertRequest;
import com.example.backend.controllers.dto.foottraffic.FootTrafficRecomputeApiResponse;
import com.example.backend.services.admin.AdminFootTrafficService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin foot traffic")
@RestController
@RequestMapping("/api/v1/admin/foot-traffic")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class AdminFootTrafficController {

    private final AdminFootTrafficService adminFootTrafficService;

    @GetMapping("/params")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> listParams() {
        return ResponseEntity.ok(adminFootTrafficService.listParams());
    }

    @GetMapping("/params/{archetype}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getParams(@PathVariable("archetype") String archetype) {
        try {
            return ResponseEntity.ok(adminFootTrafficService.getParams(archetype));
        } catch (NoSuchElementException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
        }
    }

    @PutMapping("/params/{archetype}")
    @PreAuthorize("hasRole('ADMIN')")
    @Audited(action = "ADMIN_FOOT_TRAFFIC_PARAMS_UPSERT")
    public ResponseEntity<?> upsertParams(
            @PathVariable("archetype") String archetype,
            @Valid @RequestBody AdminFootTrafficParamsUpsertRequest request) {
        try {
            return ResponseEntity.ok(adminFootTrafficService.upsert(archetype, request));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    @PostMapping("/recompute")
    @PreAuthorize("hasRole('ADMIN')")
    @Audited(action = "ADMIN_FOOT_TRAFFIC_RECOMPUTE")
    public ResponseEntity<FootTrafficRecomputeApiResponse> recompute(
            @RequestParam(value = "scenarioId", required = false) Integer scenarioId,
            @RequestParam(value = "h3Prefix", required = false) String h3Prefix) {
        return ResponseEntity.ok(adminFootTrafficService.recompute(scenarioId, h3Prefix));
    }

    @GetMapping("/audit/{h3Index}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> audit(@PathVariable("h3Index") String h3Index) {
        try {
            return ResponseEntity.ok(adminFootTrafficService.audit(h3Index));
        } catch (NoSuchElementException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
        }
    }
}
