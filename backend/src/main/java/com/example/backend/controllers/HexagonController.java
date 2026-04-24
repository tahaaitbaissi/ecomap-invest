package com.example.backend.controllers;

import com.example.backend.controllers.dto.HexagonMapResponse;
import com.example.backend.services.HexagonScoringService;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * H3 heatmap: public GET. Optional {@code profileId} loads drivers/competitors from
 * {@code dynamic_profile} (see Flyway V3). Without it, default tags from config are used.
 */
@RestController
@RequestMapping("/api/v1/hexagons")
@RequiredArgsConstructor
public class HexagonController {

    private final HexagonScoringService hexagonScoringService;

    @GetMapping
    public ResponseEntity<?> getHexagons(
            @RequestParam("bbox") String bbox,
            @RequestParam(value = "profileId", required = false) UUID profileId) {
        try {
            List<HexagonMapResponse> body = hexagonScoringService.getHexagonsInBbox(bbox, profileId);
            return ResponseEntity.ok()
                    .cacheControl(CacheControl.maxAge(5, TimeUnit.MINUTES))
                    .body(body);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        } catch (NoSuchElementException ex) {
            return ResponseEntity.notFound().build();
        }
    }
}
