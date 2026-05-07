package com.example.backend.services;

import com.example.backend.repositories.HexagonScoreRepository;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class HexagonScorePersistenceService {

    private final HexagonScoreRepository hexagonScoreRepository;

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void upsertViewportScores(UUID profileId, Map<String, Double> normalizedScoreByH3Index) {
        long startedAt = System.nanoTime();
        try {
            for (Map.Entry<String, Double> e : normalizedScoreByH3Index.entrySet()) {
                hexagonScoreRepository.upsert(e.getKey(), profileId, e.getValue());
            }
            log.debug(
                    "Persisted {} viewport hex scores asynchronously in {}ms",
                    normalizedScoreByH3Index.size(),
                    elapsedMs(startedAt));
        } catch (Exception e) {
            log.warn("hexagon_score async persistence failed: {}", e.getMessage());
        }
    }

    private static long elapsedMs(long startedAtNanos) {
        return (System.nanoTime() - startedAtNanos) / 1_000_000;
    }
}
