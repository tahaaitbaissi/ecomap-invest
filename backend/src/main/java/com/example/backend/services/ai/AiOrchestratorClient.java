package com.example.backend.services.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Streams a chat turn from the dedicated orchestrator service.
 *
 * <p>The backend remains the gatekeeper (JWT + profile ownership + context building). The
 * orchestrator is treated as an internal, untrusted-but-useful worker.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiOrchestratorClient {

    private final ObjectMapper objectMapper;

    private final HttpClient httpClient =
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();

    @Value("${app.ai.orchestrator.base-url:http://ai-orchestrator:8081}")
    private String baseUrl;

    @Value("${app.ai.orchestrator.enabled:true}")
    private boolean enabled;

    public boolean isEnabled() {
        return enabled;
    }

    public void streamChatTurn(AiOrchestratorChatTurnRequest payload, SseEmitter emitter) throws Exception {
        if (!enabled) {
            throw new IllegalStateException("orchestrator disabled");
        }
        String json = objectMapper.writeValueAsString(payload);
        URI uri = URI.create(baseUrl + "/internal/ai/chat/stream");
        HttpRequest req =
                HttpRequest.newBuilder(uri)
                        .timeout(Duration.ofSeconds(120))
                        .header("Accept", "text/event-stream")
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                        .build();

        HttpResponse<java.io.InputStream> res =
                httpClient.send(req, HttpResponse.BodyHandlers.ofInputStream());
        if (res.statusCode() < 200 || res.statusCode() >= 300) {
            throw new IllegalStateException("orchestrator HTTP " + res.statusCode());
        }

        try (BufferedReader br =
                new BufferedReader(new InputStreamReader(res.body(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                if (!line.startsWith("data:")) {
                    continue;
                }
                // Preserve token spacing. SSE allows an optional single space after `data:`.
                // Never strip arbitrary leading whitespace because it can be part of the token.
                String data = line.substring("data:".length());
                if (data.startsWith(" ")) {
                    data = data.substring(1);
                }
                emitter.send(SseEmitter.event().data(data));
                if ("[DONE]".equals(data)) {
                    return;
                }
            }
        }
    }
}

