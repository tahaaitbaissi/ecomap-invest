package com.example.backend.controllers;

import com.example.backend.controllers.dto.AiChatStreamRequest;
import com.example.backend.services.ai.AiChatService;
import com.example.backend.services.profile.DynamicProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Tag(name = "AI chat")
@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class AiChatController {

    private final AiChatService aiChatService;
    private final DynamicProfileService dynamicProfileService;

    @Value("${app.explain.sse-timeout-ms:180000}")
    private long sseTimeoutMs;

    @Operation(
            summary = "SSE stream chat with grounded context bundle",
            description = "Builds a deterministic context bundle (hex details + optional opportunity metrics), then proxies to ai-orchestrator; ends with data [DONE].")
    @PostMapping(path = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(Authentication authentication, @Valid @RequestBody AiChatStreamRequest request) {
        dynamicProfileService.getOwnedActiveEntity(authentication.getName(), request.profileId());
        SseEmitter emitter = new SseEmitter(sseTimeoutMs);
        aiChatService.streamChatAsync(
                authentication.getName(),
                request.profileId(),
                request.selectedHexIndex(),
                request.viewportCellCount(),
                request.conversationId(),
                request.message(),
                emitter);
        return emitter;
    }
}

