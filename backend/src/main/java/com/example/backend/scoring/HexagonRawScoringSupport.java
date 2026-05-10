package com.example.backend.scoring;

import com.example.backend.entities.Demographics;
import com.example.backend.entities.DynamicProfile;
import com.example.backend.repositories.DemographicsRepository;
import com.example.backend.repositories.DynamicProfileRepository;
import com.example.backend.repositories.H3HexagonRepository;
import com.example.backend.controllers.dto.TagWeightDto;
import com.example.backend.demographics.DeterministicDemographicsFallback;
import com.example.backend.foottraffic.config.FootTrafficProperties;
import com.example.backend.foottraffic.services.FootTrafficService;
import com.example.backend.repositories.PoiRepository;
import com.example.backend.services.profile.ProfileTagCatalog;
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
import org.springframework.stereotype.Component;

/**
 * Shared raw hex score: Σ(d×w) + P_norm×0.3 − Σ(c×w), reused by {@link
 * com.example.backend.services.HexagonScoringService} and simulation.
 */
@Component
@RequiredArgsConstructor
public class HexagonRawScoringSupport {

    private final H3Core h3;
    private final FootTrafficService footTrafficService;
    private final FootTrafficProperties footTrafficProperties;
    private final PoiRepository poiRepository;
    private final DemographicsRepository demographicsRepository;
    private final DeterministicDemographicsFallback demographicsFallback;
    private final H3HexagonRepository h3HexagonRepository;
    private final GeometryFactory geometryFactory;
    private final DynamicProfileRepository dynamicProfileRepository;
    private final ProfileTagCatalog profileTagCatalog;

    @Value("${app.poi.driver-categories:category=school,category=university,category=office}")
    private String driverCategoriesConfig;

    @Value("${app.hexagon.default-competitor-tags:}")
    private String defaultCompetitorTags;

    @Value("${app.hexagon.demographic-density-cap:20000.0}")
    private double demographicDensityCap;

    @Value("${app.hexagon.demographic-blend:0.0}")
    private double demographicBlend;

    /** Per-tag POI count inside the hex times weight (same geometry as {@link #weightedWithin}). */
    public record TagHexContribution(String tag, double weight, long countInside, double contribution) {}

    /**
     * Hex-level components: drivers + blended demand term (demographics + optional foot traffic) −
     * weighted competitors.
     */
    public record HexRawParts(
            double driversWeighted,
            double pTerm,
            double trafficTerm,
            double demandTerm,
            double competitorsWeighted) {}

    public HexRawParts computeParts(String h3Index, HexScoringConfig cfg) {
        double pTerm = demographicsPTerm(h3Index, cfg);
        double driversSum = weightedWithin(cfg.driverTags(), h3Index);
        double competitorsSum = weightedWithin(cfg.competitorTags(), h3Index);
        double trafficNorm = footTrafficService.getTrafficIntensityNorm(h3Index).orElse(0.0);
        double trafficTerm = trafficNorm * footTrafficProperties.getTermWeight();
        double demandTerm =
                trafficTerm > 0
                        ? pTerm * footTrafficProperties.getBlendAlpha()
                                + trafficTerm * (1.0 - footTrafficProperties.getBlendAlpha())
                        : pTerm;
        return new HexRawParts(driversSum, pTerm, trafficTerm, demandTerm, competitorsSum);
    }

    public double computeRaw(String h3Index, HexScoringConfig cfg) {
        HexRawParts parts = computeParts(h3Index, cfg);
        return parts.driversWeighted() + parts.demandTerm() - parts.competitorsWeighted();
    }

    public List<TagHexContribution> listDriverContributions(String h3Index, HexScoringConfig cfg) {
        return listWeightedContributions(cfg.driverTags(), h3Index);
    }

    public List<TagHexContribution> listCompetitorContributions(String h3Index, HexScoringConfig cfg) {
        return listWeightedContributions(cfg.competitorTags(), h3Index);
    }

    public long competitorCountWithinHex(String h3Index, HexScoringConfig cfg) {
        boolean useHexJoin = h3HexagonRepository.existsById(h3Index);
        String wkt = useHexJoin ? null : hexCellToPolygonWkt(h3Index);
        long total = 0;
        for (TagWeight t : cfg.competitorTags()) {
            long c =
                    useHexJoin
                            ? poiRepository.countByTypeTagWithinHex(t.tag(), h3Index)
                            : poiRepository.countByTypeTagAndWithin(t.tag(), wkt);
            total += c;
        }
        return total;
    }

    public HexScoringConfig buildConfigForProfile(UUID profileId) {
        DynamicProfile p = dynamicProfileRepository
                .findById(profileId)
                .orElseThrow(() -> new java.util.NoSuchElementException("Unknown profile: " + profileId));
        if (p.getArchivedAt() != null) {
            throw new java.util.NoSuchElementException("Unknown profile: " + profileId);
        }
        List<TagWeight> drivers = canonicalizeStoredWeights(parseTagWeights(p.getDriversConfig()), "hex-config.drivers");
        List<TagWeight> comp = canonicalizeStoredWeights(parseTagWeights(p.getCompetitorsConfig()), "hex-config.competitors");
        if (drivers.isEmpty() && comp.isEmpty()) {
            return defaultConfig();
        }
        return new HexScoringConfig(
                drivers.isEmpty() ? defaultDrivers() : drivers,
                comp.isEmpty() ? defaultCompetitors() : comp,
                demographicBlend > 0);
    }

    /** Align persisted profile JSON with the API/catalog (aliases → canonical tags). */
    private List<TagWeight> canonicalizeStoredWeights(List<TagWeight> tags, String context) {
        if (tags == null || tags.isEmpty()) {
            return List.of();
        }
        List<TagWeightDto> dtos =
                tags.stream().map(t -> new TagWeightDto(t.tag(), t.weight())).toList();
        return profileTagCatalog.canonicalize(dtos, context).stream()
                .map(t -> new TagWeight(t.tag(), t.weight()))
                .toList();
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

    /** Population term (0–~0.3) matching {@link #computeRaw} when demographics enabled. */
    public double demographicsPTerm(String h3Index, HexScoringConfig cfg) {
        if (!cfg.useDemographics()) {
            return 0.0;
        }
        Optional<Demographics> d = demographicsRepository.findById(h3Index);
        double density = demographicsFallback.populationDensity(d, h3Index);
        if (density <= 0.0) {
            return 0.0;
        }
        double pNorm = Math.min(density / Math.max(1.0, demographicDensityCap), 1.0);
        return pNorm * 0.3;
    }

    /** Weighted POI counts for profile drivers inside a geographic radius from a point. */
    public double weightedDriversNearLatLng(double lat, double lng, double radiusMeters, HexScoringConfig cfg) {
        double s = 0.0;
        for (TagWeight t : cfg.driverTags()) {
            s += poiRepository.countByTypeTagAndNearby(t.tag(), lat, lng, radiusMeters) * t.weight();
        }
        return s;
    }

    /** Total competitor-feature count near a point across all competitor tags (unweighted sum). */
    public long competitorCountNearLatLng(double lat, double lng, double radiusMeters, HexScoringConfig cfg) {
        long total = 0;
        for (TagWeight t : cfg.competitorTags()) {
            total += poiRepository.countByTypeTagAndNearby(t.tag(), lat, lng, radiusMeters);
        }
        return total;
    }

    /** Count of POIs with {@code preferredTag} within radius meters. */
    public long countNearbyByTag(double lat, double lng, double radiusMeters, String typeTag) {
        return poiRepository.countByTypeTagAndNearby(typeTag, lat, lng, radiusMeters);
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

    private double weightedWithin(List<TagWeight> tags, String h3Index) {
        double s = 0.0;
        for (TagHexContribution row : listWeightedContributions(tags, h3Index)) {
            s += row.contribution();
        }
        return s;
    }

    private List<TagHexContribution> listWeightedContributions(List<TagWeight> tags, String h3Index) {
        boolean useHexJoin = h3HexagonRepository.existsById(h3Index);
        String wkt = useHexJoin ? null : hexCellToPolygonWkt(h3Index);
        List<TagHexContribution> rows = new ArrayList<>();
        for (TagWeight t : tags) {
            long c =
                    useHexJoin
                            ? poiRepository.countByTypeTagWithinHex(t.tag(), h3Index)
                            : poiRepository.countByTypeTagAndWithin(t.tag(), wkt);
            rows.add(new TagHexContribution(t.tag(), t.weight(), c, c * t.weight()));
        }
        return rows;
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
