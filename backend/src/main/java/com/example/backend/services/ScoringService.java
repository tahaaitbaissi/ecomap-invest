package com.example.backend.services;

import com.example.backend.dto.HexagonScoreDTO;
import com.example.backend.entities.Demographics;
import com.example.backend.entities.H3Hexagon;
import com.example.backend.repositories.DemographicsRepository;
import com.example.backend.repositories.H3HexagonRepository;
import com.example.backend.repositories.PoiRepository;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScoringService {

    private final H3HexagonRepository hexagonRepository;
    private final PoiRepository poiRepository;
    private final DemographicsRepository demographicsRepository;
    private final GeometryFactory geometryFactory = new GeometryFactory();

    public List<HexagonScoreDTO> getScoresForBBox(Geometry bbox) {
        List<H3Hexagon> hexagons = hexagonRepository.findAllInBBox(bbox);
        return hexagons.stream().map(hex -> {
            double score = calculateScoreForGeometry(hex.getBoundary(), hex.getH3Index());
            return new HexagonScoreDTO(hex.getH3Index(), score, getColorForScore(score));
        }).collect(Collectors.toList());
    }

    public Double calculatePointScore(Double lat, Double lng, String type) {
        Point point = geometryFactory.createPoint(new Coordinate(lng, lat));
        return calculateScoreForGeometry(point.buffer(0.005), "simulated"); 
    }

    private double calculateScoreForGeometry(Geometry geom, String h3Index) {
        long drivers = poiRepository.countByTypeInGeometry(geom, List.of("school", "office", "university"));
        long competitors = poiRepository.countByTypeInGeometry(geom, List.of("cafe", "bakery", "restaurant"));
        
        Demographics demo = demographicsRepository.findByH3Index(h3Index).orElse(null);
        double popScore = (demo != null) ? demo.getPopulationDensity() / 200.0 : 0;

        double rawScore = (drivers * 1.5) + (popScore * 0.3) - (competitors * 2.0);
        return Math.max(0, Math.min(100, 50 + rawScore));
    }

    private String getColorForScore(double score) {
        if (score > 60) return "GREEN";
        if (score > 30) return "YELLOW";
        return "RED";
    }
}