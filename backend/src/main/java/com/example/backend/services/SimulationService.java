package com.example.backend.services;

import com.example.backend.dto.HexagonScoreDTO;
import com.example.backend.entities.Simulation;
import com.example.backend.repositories.SimulationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Comparator;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SimulationService {

    private final ScoringService scoringService;
    private final SimulationRepository simulationRepository;

    public Double simulateNewPoint(Double lat, Double lng, String type) {
        return scoringService.calculatePointScore(lat, lng, type);
    }

    public Simulation saveSimulation(Simulation sim) {
        return simulationRepository.save(sim);
    }

    public List<HexagonScoreDTO> getTopRecommendations(List<HexagonScoreDTO> allScores) {
        return allScores.stream()
                .sorted(Comparator.comparing(HexagonScoreDTO::getScore).reversed())
                .limit(3)
                .collect(Collectors.toList());
    }
}