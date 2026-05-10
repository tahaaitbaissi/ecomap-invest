package com.example.backend.services;

import com.example.backend.controllers.dto.TopPoiDto;
import com.example.backend.controllers.dto.ZoneStatsResponse;
import com.example.backend.entities.Demographics;
import com.example.backend.entities.DynamicProfile;
import com.example.backend.entities.Poi;
import com.example.backend.foottraffic.entities.FootTrafficCellProfile;
import com.example.backend.foottraffic.services.FootTrafficService;
import com.example.backend.demographics.DeterministicDemographicsFallback;
import com.example.backend.repositories.DemographicsRepository;
import com.example.backend.repositories.H3HexagonRepository;
import com.example.backend.repositories.PoiRepository;
import com.example.backend.services.profile.DynamicProfileService;
import com.fasterxml.jackson.databind.JsonNode;
import com.uber.h3core.H3Core;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private static final int TOP_POI_LIMIT = 20;

    private final H3Core h3;
    private final H3HexagonRepository h3HexagonRepository;
    private final PoiRepository poiRepository;
    private final DemographicsRepository demographicsRepository;
    private final DynamicProfileService dynamicProfileService;
    private final FootTrafficService footTrafficService;
    private final DeterministicDemographicsFallback demographicsFallback;

    @Value("${app.hexagon.resolution:9}")
    private int gridResolution = 9;

    @Transactional(readOnly = true)
    public ZoneStatsResponse getZoneStatistics(String h3Index, UUID profileId, String userEmail) {
        if (h3Index == null || h3Index.isBlank()) {
            throw new IllegalArgumentException("h3Index must be provided");
        }
        if (profileId == null) {
            throw new IllegalArgumentException("profileId must be provided");
        }
        if (userEmail == null || userEmail.isBlank()) {
            throw new IllegalArgumentException("userEmail must be provided");
        }

        DynamicProfile profile = dynamicProfileService.getOwnedActiveEntity(userEmail, profileId);

        List<TagWeight> drivers = parseTagWeights(profile.getDriversConfig());
        List<TagWeight> competitors = parseTagWeights(profile.getCompetitorsConfig());

        List<String> leavesOnGrid = expandToLeavesOnSeedGrid(h3Index.trim());
        if (leavesOnGrid.isEmpty()) {
            // Not part of the study grid; return empty analytics instead of "0 everywhere".
            return new ZoneStatsResponse(h3Index.trim(), profileId, null, null, Map.of(), Map.of(), List.of());
        }

        Map<String, Integer> driverCounts = new LinkedHashMap<>();
        for (TagWeight t : drivers) {
            long sum = 0;
            for (String leaf : leavesOnGrid) {
                sum += poiRepository.countByTypeTagWithinHex(t.tag(), leaf);
            }
            driverCounts.put(t.tag(), toBoundedInt(sum));
        }

        Map<String, Integer> competitorCounts = new LinkedHashMap<>();
        for (TagWeight t : competitors) {
            long sum = 0;
            for (String leaf : leavesOnGrid) {
                sum += poiRepository.countByTypeTagWithinHex(t.tag(), leaf);
            }
            competitorCounts.put(t.tag(), toBoundedInt(sum));
        }

        Double popDensity = averagePopDensity(leavesOnGrid);

        int totalDrivers = driverCounts.values().stream().mapToInt(Integer::intValue).sum();
        Integer estimatedDailyPedestrians = estimateDailyPedestrians(leavesOnGrid, popDensity, totalDrivers);

        List<TopPoiDto> topDtos = topPoisAcrossLeaves(leavesOnGrid);

        return new ZoneStatsResponse(
                h3Index.trim(),
                profileId,
                popDensity,
                estimatedDailyPedestrians,
                driverCounts,
                competitorCounts,
                topDtos);
    }

    /**
     * Mean {@code baseline_daily} from stored foot-traffic profiles (SOAP or in-process recompute); if
     * none exist yet, fall back to a simple heuristic based on population density + driver POI counts.
     */
    private Integer estimateDailyPedestrians(List<String> leavesOnGrid, Double populationDensity, int totalDriverCount) {
        double sum = 0.0;
        int n = 0;
        for (String leaf : leavesOnGrid) {
            Optional<FootTrafficCellProfile> prof = footTrafficService.getProfile(leaf);
            if (prof.isPresent()) {
                sum += prof.get().getBaselineDaily();
                n++;
            }
        }
        if (n > 0) {
            return (int) Math.round(sum / n);
        }
        double p = populationDensity == null ? 0.0 : populationDensity;
        double raw = p * 0.4 + totalDriverCount * 200.0;
        int est = (int) Math.round(raw);
        return Math.min(Math.max(est, 0), 50_000);
    }

    private static int toBoundedInt(long v) {
        if (v <= 0) return 0;
        if (v >= Integer.MAX_VALUE) return Integer.MAX_VALUE;
        return (int) v;
    }

    private static List<TagWeight> parseTagWeights(JsonNode root) {
        if (root == null || !root.isArray() || root.isEmpty()) {
            return List.of();
        }
        List<TagWeight> out = new ArrayList<>();
        for (JsonNode n : root) {
            if (n == null || !n.isObject()) continue;
            String tag = n.hasNonNull("tag") ? n.get("tag").asText() : "";
            if (tag.isBlank()) continue;
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
            out.add(new TagWeight(tag.trim(), w));
        }
        return out;
    }

    private record TagWeight(String tag, double weight) {}

    private List<String> expandToLeavesOnSeedGrid(String h3Index) {
        long cell = h3.stringToH3(h3Index);
        int r = h3.getResolution(cell);
        if (r > gridResolution) {
            // finer than our study grid: only accept if the exact cell exists in the seeded grid
            return h3HexagonRepository.existsById(h3Index) ? List.of(h3Index) : List.of();
        }
        if (r == gridResolution) {
            return h3HexagonRepository.existsById(h3Index) ? List.of(h3Index) : List.of();
        }
        List<Long> children = h3.cellToChildren(cell, gridResolution);
        List<String> out = new ArrayList<>();
        for (Long idx : children) {
            String s = h3.h3ToString(idx);
            if (h3HexagonRepository.existsById(s)) {
                out.add(s);
            }
        }
        return out;
    }

    private Double averagePopDensity(List<String> leavesOnGrid) {
        double sum = 0.0;
        int n = 0;
        for (String leaf : leavesOnGrid) {
            Optional<Demographics> demo = demographicsRepository.findById(leaf);
            double p = demographicsFallback.populationDensity(demo, leaf);
            if (p > 0.0) {
                sum += p;
                n++;
            }
        }
        return n == 0 ? null : sum / n;
    }

    private List<TopPoiDto> topPoisAcrossLeaves(List<String> leavesOnGrid) {
        // Res-9 leaf: preserve existing semantics.
        if (leavesOnGrid.size() == 1) {
            List<Poi> top = poiRepository.findTopPoisWithinHex(leavesOnGrid.getFirst(), TOP_POI_LIMIT);
            return top.stream().map(p -> new TopPoiDto(p.getName(), p.getTypeTag(), p.getAddress())).toList();
        }
        // Aggregated parent: merge child top lists (simple concat + dedup + limit).
        List<TopPoiDto> out = new ArrayList<>();
        java.util.HashSet<String> seen = new java.util.HashSet<>();
        for (String leaf : leavesOnGrid) {
            List<Poi> top = poiRepository.findTopPoisWithinHex(leaf, 5);
            for (Poi p : top) {
                String k = (p.getName() == null ? "" : p.getName()) + "|" + p.getTypeTag() + "|" + (p.getAddress() == null ? "" : p.getAddress());
                if (seen.add(k)) {
                    out.add(new TopPoiDto(p.getName(), p.getTypeTag(), p.getAddress()));
                    if (out.size() >= TOP_POI_LIMIT) {
                        return out;
                    }
                }
            }
        }
        return out;
    }
}

