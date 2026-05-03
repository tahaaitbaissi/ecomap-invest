package com.example.backend.services;

import com.example.backend.repositories.HexagonScoreRepository;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HexagonScorePersistenceService {

    private final HexagonScoreRepository hexagonScoreRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void upsertViewportScores(UUID profileId, Map<String, Double> normalizedScoreByH3Index) {
        for (Map.Entry<String, Double> e : normalizedScoreByH3Index.entrySet()) {
            hexagonScoreRepository.upsert(e.getKey(), profileId, e.getValue());
        }
    }
}
