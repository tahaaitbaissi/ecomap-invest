package com.example.backend.controllers;

import com.example.backend.services.explain.HexScoreExplanationService;
import com.example.backend.services.profile.DynamicProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Tag(name = "AI explain")
@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class AiExplainController {

    private final HexScoreExplanationService hexScoreExplanationService;
    private final DynamicProfileService dynamicProfileService;

    @Value("${app.explain.sse-timeout-ms:180000}")
    private long sseTimeoutMs;

    @Operation(
            summary = "SSE stream explaining a hex score",
            description = "French narrative grounded in deterministic hex scoring facts; ends with data [DONE].")
    @GetMapping(path = "/explain", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter explainHexScore(
            Authentication authentication,
            @RequestParam("h3Index") String h3Index,
            @RequestParam("profileId") UUID profileId,
            @RequestParam(value = "viewportCellCount", required = false) Integer viewportCellCount) {
        dynamicProfileService.getOwnedActiveEntity(authentication.getName(), profileId);
        SseEmitter emitter = new SseEmitter(sseTimeoutMs);
        hexScoreExplanationService.streamExplanationAsync(profileId, h3Index, viewportCellCount, emitter);
        return emitter;
    }
}
