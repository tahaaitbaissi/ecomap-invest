package com.example.aiorchestrator.services;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationMemoryService {

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    @Value("${app.ai.memory.ttl-seconds:3600}")
    private long ttlSeconds;

    @Value("${app.ai.memory.max-turns:12}")
    private int maxTurns;

    public List<ChatTurn> load(String conversationId) {
        String key = key(conversationId);
        String raw = redis.opsForValue().get(key);
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        try {
            List<ChatTurn> turns =
                    objectMapper.readValue(raw, new TypeReference<List<ChatTurn>>() {});
            return turns != null ? turns : List.of();
        } catch (Exception e) {
            log.warn("Failed to parse memory for {}: {}", conversationId, e.getMessage());
            return List.of();
        }
    }

    public void append(String conversationId, ChatTurn turn) {
        List<ChatTurn> turns = new ArrayList<>(load(conversationId));
        turns.add(turn);
        int max = Math.max(2, maxTurns);
        if (turns.size() > max) {
            turns = turns.subList(turns.size() - max, turns.size());
        }
        try {
            String json = objectMapper.writeValueAsString(turns);
            String key = key(conversationId);
            redis.opsForValue().set(key, json, Duration.ofSeconds(Math.max(30, ttlSeconds)));
        } catch (Exception e) {
            log.warn("Failed to write memory for {}: {}", conversationId, e.getMessage());
        }
    }

    private static String key(String conversationId) {
        return "ai:chat:" + conversationId;
    }

    public record ChatTurn(String user, String assistant) {}
}

