package com.example.backend.controllers;

import com.example.backend.dto.HexagonScoreDTO;
import com.example.backend.entities.Simulation;
import com.example.backend.services.ScoringService;
import com.example.backend.services.SimulationService;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.WKTReader;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/map")
@RequiredArgsConstructor
public class MapController {

    private final ScoringService scoringService;
    private final SimulationService simulationService;

    @GetMapping("/hexagons")
    public List<HexagonScoreDTO> getHexagons(@RequestParam String bboxWkt) throws Exception {
        Geometry bbox = new WKTReader().read(bboxWkt);
        return scoringService.getScoresForBBox(bbox);
    }

    @PostMapping("/simulate")
    public Double simulate(@RequestParam Double lat, @RequestParam Double lng, @RequestParam String type) {
        return simulationService.simulateNewPoint(lat, lng, type);
    }

    @PostMapping("/save-simulation")
    public Simulation save(@RequestBody Simulation sim) {
        return simulationService.saveSimulation(sim);
    }

    @GetMapping("/recommendations")
    public List<HexagonScoreDTO> getRecommendations(@RequestParam String bboxWkt) throws Exception {
        Geometry bbox = new WKTReader().read(bboxWkt);
        List<HexagonScoreDTO> scores = scoringService.getScoresForBBox(bbox);
        return simulationService.getTopRecommendations(scores);
    }
}