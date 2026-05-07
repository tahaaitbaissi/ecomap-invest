package com.example.aiorchestrator.controllers;

import com.example.aiorchestrator.dto.AiOrchestratorChatTurnRequest;
import com.example.aiorchestrator.services.ChatTurnWorkflowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/internal/ai/chat")
@RequiredArgsConstructor
public class InternalAiChatController {

    private final ChatTurnWorkflowService workflow;

    @Value("${app.ai.sse-timeout-ms:180000}")
    private long sseTimeoutMs;

    @PostMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@Valid @RequestBody AiOrchestratorChatTurnRequest request) {
        SseEmitter emitter = new SseEmitter(sseTimeoutMs);
        workflow.stream(request, emitter);
        return emitter;
    }
}

