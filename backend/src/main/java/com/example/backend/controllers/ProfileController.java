package com.example.backend.controllers;

import com.example.backend.controllers.dto.DynamicProfileResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.example.backend.controllers.dto.GenerateDynamicProfileRequest;
import com.example.backend.services.profile.DynamicProfileService;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@Tag(name = "Dynamic profiles", description = "AI-assisted investment profiles")
@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class ProfileController {

    private final DynamicProfileService dynamicProfileService;

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
            @Parameter(hidden = true)
                    @PageableDefault(size = 20, sort = "generatedAt", direction = Sort.Direction.DESC)
                    Pageable pageable) {
        return ResponseEntity.ok(dynamicProfileService.listMine(authentication.getName(), pageable));
    }

    @Operation(summary = "Get one profile by id (must belong to caller)")
    @GetMapping("/{id}")
    public ResponseEntity<DynamicProfileResponse> getOne(
            Authentication authentication,
            @PathVariable("id") UUID id) {
        return ResponseEntity.ok(dynamicProfileService.getMineById(authentication.getName(), id));
    }
}

