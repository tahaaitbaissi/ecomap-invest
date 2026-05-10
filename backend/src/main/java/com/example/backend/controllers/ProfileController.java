package com.example.backend.controllers;

import com.example.backend.controllers.dto.DynamicProfileResponse;
import com.example.backend.controllers.dto.HexExplanationContextDto;
import com.example.backend.controllers.dto.ProfileScoreNormDebugResponse;
import com.example.backend.controllers.dto.ProfileTagOptionResponse;
import com.example.backend.controllers.dto.UpdateDynamicProfileRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.example.backend.controllers.dto.GenerateDynamicProfileRequest;
import com.example.backend.scoring.HexagonRawScoringSupport;
import com.example.backend.services.ProfileScoreScaleService;
import com.example.backend.services.explain.HexExplanationContextBuilder;
import com.example.backend.services.profile.DynamicProfileService;
import com.example.backend.services.profile.ProfileTagCatalog;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@Tag(name = "Dynamic profiles", description = "AI-assisted investment profiles")
@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('INVESTOR','ADMIN')")
public class ProfileController {

    private final DynamicProfileService dynamicProfileService;
    private final ProfileTagCatalog profileTagCatalog;
    private final HexagonRawScoringSupport hexagonRawScoringSupport;
    private final ProfileScoreScaleService profileScoreScaleService;
    private final HexExplanationContextBuilder hexExplanationContextBuilder;

    @Operation(summary = "Generate a dynamic profile from a natural-language query (LLM + fallback)")
    @PostMapping("/generate")
    public ResponseEntity<DynamicProfileResponse> generate(
            Authentication authentication,
            @Valid @RequestBody GenerateDynamicProfileRequest request,
            UriComponentsBuilder uriBuilder) {
        DynamicProfileResponse created =
                dynamicProfileService.generateForUserEmail(authentication.getName(), request.query());
        URI location = uriBuilder.path("/api/v1/profile/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @Operation(summary = "List profiles for the current user (paged, newest first)")
    @GetMapping("/my")
    public ResponseEntity<Page<DynamicProfileResponse>> my(
            Authentication authentication,
            @Parameter(description = "Include archived profiles in management views")
                    @org.springframework.web.bind.annotation.RequestParam(value = "includeArchived", defaultValue = "false")
                    boolean includeArchived,
            @Parameter(hidden = true)
                    @PageableDefault(size = 20, sort = "generatedAt", direction = Sort.Direction.DESC)
                    Pageable pageable) {
        return ResponseEntity.ok(dynamicProfileService.listMine(authentication.getName(), pageable, includeArchived));
    }

    @Operation(summary = "List supported profile rule tags")
    @GetMapping("/tags")
    public ResponseEntity<List<ProfileTagOptionResponse>> tags() {
        return ResponseEntity.ok(profileTagCatalog.options());
    }

    @Operation(summary = "Get one profile by id (must belong to caller)")
    @GetMapping("/{id}")
    public ResponseEntity<DynamicProfileResponse> getOne(
            Authentication authentication,
            @PathVariable("id") UUID id) {
        return ResponseEntity.ok(dynamicProfileService.getMineById(authentication.getName(), id));
    }

    @Operation(
            summary = "Hex scoring facts for UI and LLM grounding",
            description =
                    "Deterministic breakdown for the selected H3 cell (or aggregated parent cell) under this profile,"
                            + " including POI-tag contributions and normalization stretch bounds.")
    @GetMapping("/{id}/hex-details")
    public ResponseEntity<?> hexDetails(
            Authentication authentication,
            @PathVariable("id") UUID id,
            @RequestParam("h3Index") String h3Index,
            @Parameter(description = "Matches heatmap normalization viewport cell count when available")
                    @RequestParam(value = "viewportCellCount", required = false)
                    Integer viewportCellCount) {
        dynamicProfileService.getMineById(authentication.getName(), id);
        try {
            HexExplanationContextDto body =
                    hexExplanationContextBuilder.build(id, h3Index, viewportCellCount);
            return ResponseEntity.ok(body);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
        }
    }

    @Operation(
            summary = "Debug heatmap normalization for a profile",
            description =
                    "Returns raw-score min/max/percentiles and the stretch bounds used for 0–100 display,"
                            + " recomputed over the reference H3 sample (same basis as normalization).")
    @GetMapping("/{id}/score-norm-debug")
    public ResponseEntity<ProfileScoreNormDebugResponse> scoreNormDebug(
            Authentication authentication, @PathVariable("id") UUID id) {
        dynamicProfileService.getMineById(authentication.getName(), id);
        var cfg = hexagonRawScoringSupport.buildConfigForProfile(id);
        return ResponseEntity.ok(profileScoreScaleService.computeNormDebug(id, cfg));
    }

    @Operation(summary = "Update one profile (rename and optionally edit tag weights)")
    @PatchMapping("/{id}")
    public ResponseEntity<DynamicProfileResponse> update(
            Authentication authentication,
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateDynamicProfileRequest request) {
        return ResponseEntity.ok(dynamicProfileService.updateMine(authentication.getName(), id, request));
    }

    @Operation(summary = "Duplicate one profile for editing")
    @PostMapping("/{id}/duplicate")
    public ResponseEntity<DynamicProfileResponse> duplicate(
            Authentication authentication,
            @PathVariable("id") UUID id,
            UriComponentsBuilder uriBuilder) {
        DynamicProfileResponse created = dynamicProfileService.duplicateMine(authentication.getName(), id);
        URI location = uriBuilder.path("/api/v1/profile/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @Operation(summary = "Archive one profile (soft delete)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> archive(
            Authentication authentication,
            @PathVariable("id") UUID id) {
        dynamicProfileService.archiveMine(authentication.getName(), id);
        return ResponseEntity.noContent().build();
    }
}

