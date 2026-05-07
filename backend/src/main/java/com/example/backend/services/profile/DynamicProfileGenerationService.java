package com.example.backend.services.profile;

import com.example.backend.controllers.dto.TagWeightDto;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DynamicProfileGenerationService {

    public static final double MIN_WEIGHT = 0.1;
    public static final double MAX_WEIGHT = 1.5;

    private final FallbackProfileProvider fallbackProfileProvider;
    private final LLMService llmService;
    private final ProfileTagCatalog profileTagCatalog;

    public FallbackProfileProvider.ProfileConfig generate(String userQuery) {
        FallbackProfileProvider.ProfileConfig raw =
                llmService.generateProfileConfig(userQuery).orElseGet(() -> fallbackProfileProvider.findBestMatch(userQuery));
        List<TagWeightDto> drivers = validateAndNormalize(raw.drivers(), "llm.drivers");
        List<TagWeightDto> competitors = validateAndNormalize(raw.competitors(), "llm.competitors");
        if (drivers.isEmpty() || competitors.isEmpty()) {
            // Partial fallback instead of wiping good output: only fill the missing side(s).
            FallbackProfileProvider.ProfileConfig def = fallbackProfileProvider.findBestMatch("");
            List<TagWeightDto> defDrivers = validateAndNormalize(def.drivers(), "fallback.drivers");
            List<TagWeightDto> defCompetitors = validateAndNormalize(def.competitors(), "fallback.competitors");
            if (drivers.isEmpty()) {
                drivers = defDrivers;
            }
            if (competitors.isEmpty()) {
                competitors = defCompetitors;
            }
        }

        // Prevent the same canonical tag from appearing in both lists (netting cancels intent).
        Set<String> competitorTags = new HashSet<>();
        for (TagWeightDto c : competitors) {
            if (c != null && c.tag() != null) {
                competitorTags.add(c.tag());
            }
        }
        drivers = drivers.stream().filter(d -> d != null && d.tag() != null && !competitorTags.contains(d.tag())).toList();

        // Ensure minimum viable coverage if overlap removal made a side empty.
        if (drivers.isEmpty() || competitors.isEmpty()) {
            FallbackProfileProvider.ProfileConfig def = fallbackProfileProvider.findBestMatch("");
            List<TagWeightDto> defDrivers = validateAndNormalize(def.drivers(), "fallback.drivers");
            List<TagWeightDto> defCompetitors = validateAndNormalize(def.competitors(), "fallback.competitors");
            if (drivers.isEmpty()) {
                drivers = defDrivers;
            }
            if (competitors.isEmpty()) {
                competitors = defCompetitors;
            }
        }

        return new FallbackProfileProvider.ProfileConfig(drivers, competitors);
    }

    private List<TagWeightDto> validateAndNormalize(List<TagWeightDto> items, String context) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        List<TagWeightDto> normalized = items.stream()
                .filter(t -> t != null && t.tag() != null && !t.tag().trim().isEmpty())
                .map(t -> new TagWeightDto(
                        t.tag().trim(),
                        clampWeight(t.weight() == null ? 1.0 : t.weight())))
                .toList();
        return profileTagCatalog.canonicalize(normalized, context);
    }

    private static double clampWeight(double w) {
        if (!Double.isFinite(w)) {
            return 1.0;
        }
        if (w < MIN_WEIGHT) {
            return MIN_WEIGHT;
        }
        if (w > MAX_WEIGHT) {
            return MAX_WEIGHT;
        }
        return w;
    }
}

