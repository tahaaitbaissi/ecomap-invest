package com.example.backend.services.profile;

import com.example.backend.controllers.dto.TagWeightDto;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DynamicProfileGenerationService {

    public static final double MIN_WEIGHT = 0.1;
    public static final double MAX_WEIGHT = 1.5;

    private final FallbackProfileProvider fallbackProfileProvider;
    private final LLMService llmService;

    public FallbackProfileProvider.ProfileConfig generate(String userQuery) {
        FallbackProfileProvider.ProfileConfig raw =
                llmService.generateProfileConfig(userQuery).orElseGet(() -> fallbackProfileProvider.findBestMatch(userQuery));
        List<TagWeightDto> drivers = validateAndNormalize(raw.drivers());
        List<TagWeightDto> competitors = validateAndNormalize(raw.competitors());
        if (drivers.isEmpty() || competitors.isEmpty()) {
            FallbackProfileProvider.ProfileConfig def = fallbackProfileProvider.findBestMatch("");
            drivers = validateAndNormalize(def.drivers());
            competitors = validateAndNormalize(def.competitors());
        }
        return new FallbackProfileProvider.ProfileConfig(drivers, competitors);
    }

    private static List<TagWeightDto> validateAndNormalize(List<TagWeightDto> items) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        List<TagWeightDto> normalized = items.stream()
                .filter(t -> t != null && t.tag() != null && !t.tag().trim().isEmpty())
                .map(t -> new TagWeightDto(
                        t.tag().trim(),
                        clampWeight(t.weight() == null ? 1.0 : t.weight())))
                .toList();
        return FallbackProfileProvider.dedupeByTagKeepMaxWeight(normalized);
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

