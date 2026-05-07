package com.example.backend.services;

import com.example.backend.entities.H3Hexagon;
import com.example.backend.repositories.H3HexagonRepository;
import com.example.backend.config.CasablancaStudyAreaProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uber.h3core.H3Core;
import com.uber.h3core.util.LatLng;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Generates and persists H3 hex rows for map/scoring. Used by {@link
 * com.example.backend.bootstrap.H3GridBootstrapRunner} for the Casablanca study area.
 *
 * <p>Seed cells follow the OSM city outline in {@code classpath:geo/casablanca_admin_city.geojson},
 * with fallback to the configured study rectangle. Missing indices are inserted on startup so gaps
 * heal after boundary or resolution changes.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class H3GridService {

    private static final int CHUNK = 500;

    private final H3Core h3;
    private final H3HexagonRepository h3HexagonRepository;
    private final GeometryFactory geometryFactory;
    private final TransactionTemplate transactionTemplate;
    private final CasablancaStudyAreaProperties casablancaStudyArea;
    private final ObjectMapper objectMapper;

    @Value("${app.hexagon.resolution:9}")
    private int h3Resolution;

    /**
     * Ensures every H3 cell overlapping the Casablanca study outline exists in {@code h3_hexagon}.
     * Safe on non-empty DBs: only missing indices are inserted.
     */
    public void generateAndPersistCasablancaIfEmpty() {
        List<LatLng> ring = adminBoundaryRingOrFallback();
        List<Long> distinct = polygonToDistinctCells(ring);
        if (distinct.isEmpty()) {
            log.warn(
                    "H3 grid: polygonToCells returned no cells for admin outline; falling back to study rectangle");
            ring = studyAreaRing();
            distinct = polygonToDistinctCells(ring);
        }
        if (distinct.isEmpty()) {
            log.warn("H3 grid: polygonToCells still empty after fallback");
            return;
        }

        double swLng = casablancaStudyArea.swLng();
        double swLat = casablancaStudyArea.swLat();
        double neLng = casablancaStudyArea.neLng();
        double neLat = casablancaStudyArea.neLat();

        List<String> allIds = distinct.stream().map(h3::h3ToString).toList();
        Set<String> present = new HashSet<>();
        for (int i = 0; i < allIds.size(); i += CHUNK) {
            int end = Math.min(i + CHUNK, allIds.size());
            h3HexagonRepository.findAllById(allIds.subList(i, end)).forEach(h -> present.add(h.getH3Index()));
        }

        List<H3Hexagon> missing = new ArrayList<>();
        for (Long cell : distinct) {
            String idx = h3.h3ToString(cell);
            if (!present.contains(idx)) {
                H3Hexagon row = new H3Hexagon();
                row.setH3Index(idx);
                row.setResolution(h3Resolution);
                row.setBoundary(toPolygon(h3.cellToBoundary(cell)));
                missing.add(row);
            }
        }

        if (missing.isEmpty()) {
            log.info(
                    "H3 study grid complete: {} res-{} cells expected (0 new rows; {} already in DB)",
                    distinct.size(),
                    h3Resolution,
                    present.size());
            return;
        }

        for (int i = 0; i < missing.size(); i += CHUNK) {
            int end = Math.min(i + CHUNK, missing.size());
            List<H3Hexagon> sub = missing.subList(i, end);
            transactionTemplate.executeWithoutResult(status -> h3HexagonRepository.saveAll(sub));
        }
        log.info(
                "H3 study grid: inserted {} new hex row(s) (resolution {}), envelope sw=({},{}), ne=({},{}) — expected {} cells",
                missing.size(),
                h3Resolution,
                swLng,
                swLat,
                neLng,
                neLat,
                distinct.size());
    }

    private List<LatLng> studyAreaRing() {
        List<LatLng> ring = new ArrayList<>(4);
        ring.add(new LatLng(casablancaStudyArea.swLat(), casablancaStudyArea.swLng()));
        ring.add(new LatLng(casablancaStudyArea.swLat(), casablancaStudyArea.neLng()));
        ring.add(new LatLng(casablancaStudyArea.neLat(), casablancaStudyArea.neLng()));
        ring.add(new LatLng(casablancaStudyArea.neLat(), casablancaStudyArea.swLng()));
        return ring;
    }

    private List<LatLng> adminBoundaryRingOrFallback() {
        ClassPathResource res = new ClassPathResource("geo/casablanca_admin_city.geojson");
        if (!res.exists()) {
            log.warn("classpath:geo/casablanca_admin_city.geojson missing — using study-area rectangle ring");
            return studyAreaRing();
        }
        try (InputStream in = res.getInputStream()) {
            JsonNode coords = polygonOuterRingCoordinates(objectMapper.readTree(in));
            if (coords == null || !coords.isArray() || coords.size() < 4) {
                log.warn("Invalid admin polygon GeoJSON — using study-area rectangle ring");
                return studyAreaRing();
            }
            List<LatLng> out = new ArrayList<>(coords.size());
            for (JsonNode p : coords) {
                if (!p.isArray() || p.size() < 2) {
                    continue;
                }
                out.add(new LatLng(p.get(1).asDouble(), p.get(0).asDouble()));
            }
            closeRing(out);
            if (out.size() < 4) {
                return studyAreaRing();
            }
            return out;
        } catch (IOException e) {
            log.warn("Could not read admin polygon GeoJSON — {}", e.toString());
            return studyAreaRing();
        }
    }

    private static JsonNode polygonOuterRingCoordinates(JsonNode root) {
        String type = root.path("type").asText("");
        if ("Polygon".equals(type)) {
            JsonNode c = root.get("coordinates");
            if (c != null && c.isArray() && c.size() > 0) {
                return c.get(0);
            }
        }
        if ("Feature".equals(type)) {
            return polygonOuterRingCoordinates(root.path("geometry"));
        }
        return null;
    }

    private static void closeRing(List<LatLng> ring) {
        if (ring.size() < 2) {
            return;
        }
        LatLng f = ring.get(0);
        LatLng last = ring.get(ring.size() - 1);
        if (Double.compare(f.lat, last.lat) != 0 || Double.compare(f.lng, last.lng) != 0) {
            ring.add(new LatLng(f.lat, f.lng));
        }
    }

    private List<Long> polygonToDistinctCells(List<LatLng> ring) {
        List<Long> cellIndices = h3.polygonToCells(ring, List.of(), h3Resolution);
        return cellIndices.stream().distinct().toList();
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
