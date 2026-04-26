package com.example.backend.controllers;

import com.example.backend.controllers.dto.DynamicProfileResponse;
import com.example.backend.controllers.dto.GenerateDynamicProfileRequest;
import com.example.backend.services.profile.DynamicProfileService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final DynamicProfileService dynamicProfileService;

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

    @GetMapping("/my")
    public ResponseEntity<List<DynamicProfileResponse>> my(Authentication authentication) {
        return ResponseEntity.ok(dynamicProfileService.listMine(authentication.getName()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DynamicProfileResponse> getOne(
            Authentication authentication,
            @PathVariable("id") UUID id) {
        return ResponseEntity.ok(dynamicProfileService.getMineById(authentication.getName(), id));
    }
}

