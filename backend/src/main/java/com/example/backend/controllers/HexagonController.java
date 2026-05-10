package com.example.backend.controllers;

import com.example.backend.controllers.dto.HexagonMapResponse;
import com.example.backend.scoring.HexagonRawScoringSupport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.example.backend.services.HexagonScoringService;
import com.example.backend.services.profile.DynamicProfileService;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * H3 heatmap: JWT required for all requests. Optional {@code profileId} loads drivers/competitors from
 * {@code dynamic_profile}; without it cells use {@code score: null} (gray-only mode).
 */
@RestController
@RequestMapping("/api/v1/hexagons")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('INVESTOR','ADMIN')")
public class HexagonController {

    private final HexagonScoringService hexagonScoringService;
    private final DynamicProfileService dynamicProfileService;
    private final HexagonRawScoringSupport hexagonRawScoringSupport;

    @Operation(
            summary = "Hex cells for bbox",
            description =
                    "Optional profileId: omit for gray preview cells (score null). "
                            + "Optional h3Resolution (7–9, default 9): values 7–8 aggregate res-9 grid cells into parent "
                            + "H3 cells (fewer polygons when zoomed out); scores are averaged then normalized in-view. "
                            + "Aggregated parents are not written to hexagon_score (FK to res-9 grid only).")
    @GetMapping
    public ResponseEntity<?> getHexagons(
            @RequestParam("bbox") String bbox,
            @RequestParam(value = "profileId", required = false) UUID profileId,
            @RequestParam(value = "h3Resolution", required = false) Integer h3Resolution) {
        try {
            if (profileId != null) {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                dynamicProfileService.getOwnedActiveEntity(auth.getName(), profileId);
            }
            List<HexagonMapResponse> body = hexagonScoringService.getHexagonsInBbox(bbox, profileId, h3Resolution);
            return ResponseEntity.ok()
                    .cacheControl(CacheControl.maxAge(3600, TimeUnit.SECONDS))
                    .body(body);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        } catch (NoSuchElementException ex) {
            return ResponseEntity.notFound().build();
        } catch (AccessDeniedException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ex.getMessage());
        }
    }

    @Operation(summary = "Single H3 cell boundary by index (score null)")
    @GetMapping("/h3/{h3Index}")
    public ResponseEntity<?> byH3Index(@PathVariable("h3Index") String h3Index) {
        try {
            var ring = hexagonRawScoringSupport.boundaryPoints(h3Index);
            List<HexagonMapResponse.LatLng> boundary =
                    ring.stream().map(p -> new HexagonMapResponse.LatLng(p.lat(), p.lng())).toList();
            return ResponseEntity.ok(new HexagonMapResponse(h3Index, null, boundary));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

}
