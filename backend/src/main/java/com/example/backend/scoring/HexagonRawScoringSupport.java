package com.example.backend.scoring;

import com.example.backend.entities.Demographics;
import com.example.backend.entities.DynamicProfile;
import com.example.backend.repositories.DemographicsRepository;
import com.example.backend.repositories.DynamicProfileRepository;
import com.example.backend.repositories.H3HexagonRepository;
import com.example.backend.repositories.PoiRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.uber.h3core.H3Core;
import com.uber.h3core.util.LatLng;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

/**
 * Shared raw hex score: Σ(d×w) + P_norm×0.3 − Σ(c×w), reused by {@link
 * com.example.backend.services.HexagonScoringService} and simulation.
 */
@Component
@RequiredArgsConstructor
public class HexagonRawScoringSupport {

    private final H3Core h3;
    private final PoiRepository poiRepository;
    private final DemographicsRepository demographicsRepository;
    private final H3HexagonRepository h3HexagonRepository;
    private final GeometryFactory geometryFactory;
    private final DynamicProfileRepository dynamicProfileRepository;

    @Value("${app.poi.driver-categories:category=school,category=university,category=office}")
    private String driverCategoriesConfig;

    @Value("${app.hexagon.default-competitor-tags:}")
    private String defaultCompetitorTags;

    @Value("${app.hexagon.demographic-density-cap:20000.0}")
    private double demographicDensityCap;

    @Value("${app.hexagon.demographic-blend:0.0}")
    private double demographicBlend;

    public double computeRaw(String h3Index, HexScoringConfig cfg) {
        double pNorm = 0.0;
        if (cfg.useDemographics()) {
            Optional<Demographics> d = demographicsRepository.findById(h3Index);
            if (d.isPresent() && d.get().getPopulationDensity() != null) {
                pNorm = Math.min(d.get().getPopulationDensity() / Math.max(1.0, demographicDensityCap), 1.0);
            }
        }
        double pTerm = pNorm * 0.3;

        boolean useHexJoin = h3HexagonRepository.existsById(h3Index);
        String wkt = useHexJoin ? null : hexCellToPolygonWkt(h3Index);

        double driversSum = weightedWithin(cfg.driverTags(), useHexJoin, h3Index, wkt);
        double competitorsSum = weightedWithin(cfg.competitorTags(), useHexJoin, h3Index, wkt);

        return driversSum + pTerm - competitorsSum;
    }

    public HexScoringConfig buildConfigForProfile(UUID profileId) {
        DynamicProfile p = dynamicProfileRepository
                .findById(profileId)
                .orElseThrow(() -> new java.util.NoSuchElementException("Unknown profile: " + profileId));
        List<TagWeight> drivers = parseTagWeights(p.getDriversConfig());
        List<TagWeight> comp = parseTagWeights(p.getCompetitorsConfig());
        if (drivers.isEmpty() && comp.isEmpty()) {
            return defaultConfig();
        }
        return new HexScoringConfig(
                drivers.isEmpty() ? defaultDrivers() : drivers,
                comp.isEmpty() ? defaultCompetitors() : comp,
                demographicBlend > 0);
    }

    public HexScoringConfig defaultConfig() {
        return new HexScoringConfig(defaultDrivers(), defaultCompetitors(), demographicBlend > 0);
    }

    /** Boundary vertices for API/geo JSON (same order as hex map response). */
    public List<LatLngRingPoint> boundaryPoints(String h3Index) {
        long cell = h3.stringToH3(h3Index);
        return h3.cellToBoundary(cell).stream().map(p -> new LatLngRingPoint(p.lat, p.lng)).toList();
    }

    public Optional<Double> findDriverWeight(HexScoringConfig cfg, String tag) {
        return cfg.driverTags().stream()
                .filter(t -> t.tag().equals(tag))
                .map(TagWeight::weight)
                .findFirst();
    }

    public Optional<Double> findCompetitorWeight(HexScoringConfig cfg, String tag) {
        return cfg.competitorTags().stream()
                .filter(t -> t.tag().equals(tag))
                .map(TagWeight::weight)
                .findFirst();
    }

    private List<TagWeight> defaultDrivers() {
        return java.util.stream.Stream.of(driverCategoriesConfig.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(s -> new TagWeight(s, 1.0))
                .toList();
    }

    private List<TagWeight> defaultCompetitors() {
        if (defaultCompetitorTags == null || defaultCompetitorTags.isBlank()) {
            return List.of(new TagWeight("amenity=restaurant", 1.0));
        }
        return java.util.stream.Stream.of(defaultCompetitorTags.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(s -> new TagWeight(s, 1.0))
                .toList();
    }

    static List<TagWeight> parseTagWeights(JsonNode root) {
        if (root == null || !root.isArray() || root.isEmpty()) {
            return List.of();
        }
        List<TagWeight> out = new ArrayList<>();
        for (JsonNode n : root) {
            if (n == null || !n.isObject()) {
                continue;
            }
            String tag = n.hasNonNull("tag") ? n.get("tag").asText() : "";
            if (tag.isBlank()) {
                continue;
            }
            double w = 1.0;
            if (n.has("weight") && n.get("weight").isNumber()) {
                w = n.get("weight").asDouble(1.0);
            } else if (n.has("weight") && n.get("weight").isTextual()) {
                try {
                    w = Double.parseDouble(n.get("weight").asText());
                } catch (NumberFormatException e) {
                    w = 1.0;
                }
            }
            out.add(new TagWeight(tag, w));
        }
        return out;
    }

    private double weightedWithin(List<TagWeight> tags, boolean useHexJoin, String h3Index, @Nullable String wkt) {
        double s = 0.0;
        for (TagWeight t : tags) {
            long c =
                    useHexJoin
                            ? poiRepository.countByTypeTagWithinHex(t.tag(), h3Index)
                            : poiRepository.countByTypeTagAndWithin(t.tag(), wkt);
            s += c * t.weight();
        }
        return s;
    }

    private String hexCellToPolygonWkt(String h3Index) {
        long cell = h3.stringToH3(h3Index);
        List<LatLng> boundary = h3.cellToBoundary(cell);
        if (boundary.isEmpty()) {
            return "POLYGON EMPTY";
        }
        int n = boundary.size();
        Coordinate[] coords = new Coordinate[n + 1];
        for (int i = 0; i < n; i++) {
            LatLng p = boundary.get(i);
            coords[i] = new Coordinate(p.lng, p.lat);
        }
        coords[n] = coords[0];
        LinearRing shell = geometryFactory.createLinearRing(coords);
        Polygon poly = geometryFactory.createPolygon(shell);
        return poly.toText();
    }

    public record LatLngRingPoint(double lat, double lng) {}
}
