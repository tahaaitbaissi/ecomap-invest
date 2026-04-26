package com.example.backend.services;

import com.example.backend.controllers.dto.TopPoiDto;
import com.example.backend.controllers.dto.ZoneStatsResponse;
import com.example.backend.entities.Demographics;
import com.example.backend.entities.DynamicProfile;
import com.example.backend.entities.Poi;
import com.example.backend.entities.User;
import com.example.backend.repositories.DemographicsRepository;
import com.example.backend.repositories.DynamicProfileRepository;
import com.example.backend.repositories.PoiRepository;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private static final int TOP_POI_LIMIT = 20;

    private final PoiRepository poiRepository;
    private final DemographicsRepository demographicsRepository;
    private final DynamicProfileRepository dynamicProfileRepository;
    private final UserService userService;

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

        User user = userService.getUserByEmail(userEmail);
        DynamicProfile profile = dynamicProfileRepository.findById(profileId)
                .orElseThrow(() -> new NoSuchElementException("Profile not found"));
        if (profile.getUserId() == null || user.getId() != profile.getUserId()) {
            throw new AccessDeniedException("You do not own this profile");
        }

        List<TagWeight> drivers = parseTagWeights(profile.getDriversConfig());
        List<TagWeight> competitors = parseTagWeights(profile.getCompetitorsConfig());

        Map<String, Integer> driverCounts = new LinkedHashMap<>();
        for (TagWeight t : drivers) {
            long c = poiRepository.countByTypeTagWithinHex(t.tag(), h3Index.trim());
            driverCounts.put(t.tag(), toBoundedInt(c));
        }

        Map<String, Integer> competitorCounts = new LinkedHashMap<>();
        for (TagWeight t : competitors) {
            long c = poiRepository.countByTypeTagWithinHex(t.tag(), h3Index.trim());
            competitorCounts.put(t.tag(), toBoundedInt(c));
        }

        Optional<Demographics> demo = demographicsRepository.findById(h3Index.trim());
        Double popDensity = demo.map(Demographics::getPopulationDensity).orElse(null);

        int totalDrivers = driverCounts.values().stream().mapToInt(Integer::intValue).sum();
        int estimatedFootTraffic = estimateFootTraffic(popDensity, totalDrivers);

        List<Poi> top = poiRepository.findTopPoisWithinHex(h3Index.trim(), TOP_POI_LIMIT);
        List<TopPoiDto> topDtos = top.stream()
                .map(p -> new TopPoiDto(p.getName(), p.getTypeTag(), p.getAddress()))
                .toList();

        return new ZoneStatsResponse(
                h3Index.trim(),
                profileId,
                popDensity,
                estimatedFootTraffic,
                driverCounts,
                competitorCounts,
                topDtos);
    }

    private static int estimateFootTraffic(Double populationDensity, int totalDriverCount) {
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
}

