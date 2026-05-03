package com.example.backend.services;

import com.example.backend.entities.H3Hexagon;
import com.example.backend.repositories.H3HexagonRepository;
import com.uber.h3core.H3Core;
import com.uber.h3core.util.LatLng;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Generates and persists H3 hex rows for map/scoring. Used by {@link
 * com.example.backend.bootstrap.H3GridBootstrapRunner} for the Casablanca study area.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class H3GridService {

    private static final double CASA_SW_LNG = -7.85;
    private static final double CASA_SW_LAT = 33.35;
    private static final double CASA_NE_LNG = -7.25;
    private static final double CASA_NE_LAT = 33.75;

    private static final int CHUNK = 500;

    private final H3Core h3;
    private final H3HexagonRepository h3HexagonRepository;
    private final GeometryFactory geometryFactory;
    private final TransactionTemplate transactionTemplate;

    @Value("${app.hexagon.resolution:9}")
    private int h3Resolution;

    /** Inserts Casablanca-area hexagons when {@code h3_hexagon} is empty (chunked {@code saveAll}). */
    public void generateAndPersistCasablancaIfEmpty() {
        if (h3HexagonRepository.count() > 0) {
            log.info("H3 grid seed skipped: h3_hexagon already has {} row(s)", h3HexagonRepository.count());
            return;
        }

        List<LatLng> ring = new ArrayList<>();
        ring.add(new LatLng(CASA_SW_LAT, CASA_SW_LNG));
        ring.add(new LatLng(CASA_SW_LAT, CASA_NE_LNG));
        ring.add(new LatLng(CASA_NE_LAT, CASA_NE_LNG));
        ring.add(new LatLng(CASA_NE_LAT, CASA_SW_LNG));

        List<Long> cellIndices = h3.polygonToCells(ring, List.of(), h3Resolution);
        if (cellIndices.isEmpty()) {
            log.warn("H3 grid seed: polygonToCells returned no cells for Casablanca bbox");
            return;
        }
        List<Long> distinct = cellIndices.stream().distinct().toList();

        List<H3Hexagon> batch = new ArrayList<>();
        for (Long cell : distinct) {
            H3Hexagon row = new H3Hexagon();
            row.setH3Index(h3.h3ToString(cell));
            row.setResolution(h3Resolution);
            row.setBoundary(toPolygon(h3.cellToBoundary(cell)));
            batch.add(row);
        }

        for (int i = 0; i < batch.size(); i += CHUNK) {
            int end = Math.min(i + CHUNK, batch.size());
            List<H3Hexagon> sub = batch.subList(i, end);
            transactionTemplate.executeWithoutResult(status -> h3HexagonRepository.saveAll(sub));
        }
        log.info(
                "H3 grid seed: inserted {} hexagons (resolution {}), bbox sw=({},{}), ne=({},{})",
                batch.size(),
                h3Resolution,
                CASA_SW_LNG,
                CASA_SW_LAT,
                CASA_NE_LNG,
                CASA_NE_LAT);
    }

    private Polygon toPolygon(List<LatLng> boundary) {
        if (boundary.isEmpty()) {
            return geometryFactory.createPolygon();
        }
        int n = boundary.size();
        Coordinate[] c = new Coordinate[n + 1];
        for (int i = 0; i < n; i++) {
            LatLng p = boundary.get(i);
            c[i] = new Coordinate(p.lng, p.lat);
        }
        c[n] = c[0];
        LinearRing shell = geometryFactory.createLinearRing(c);
        return geometryFactory.createPolygon(shell);
    }
}
