package com.example.backend.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Thin liveness probe (no infra details); safe for orchestrators without credentials.
 */
@Tag(name = "Health")
@RestController
@RequestMapping("/api/v1/health")
public class LivenessController {

    @Operation(summary = "Liveness ping")
    @GetMapping("/live")
    public ResponseEntity<Map<String, String>> live() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }
}
