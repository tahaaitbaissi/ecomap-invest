package com.example.backend.services.opportunity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/** Loads classpath {@code business_fit_rules.json}: archetype regex on profile name + preferred POI tags. */
@Slf4j
@Component
public class BusinessFitCatalog {

    private final List<ResolvedArchetype> archetypes = new ArrayList<>();

    public BusinessFitCatalog(ObjectMapper objectMapper) {
        ClassPathResource res = new ClassPathResource("business_fit_rules.json");
        if (!res.exists()) {
            log.warn("business_fit_rules.json missing — using empty fit catalog");
            return;
        }
        try (InputStream in = res.getInputStream()) {
            Wrapper w = objectMapper.readValue(in, Wrapper.class);
            if (w != null && w.archetypes != null) {
                for (ArchetypeDto dto : w.archetypes) {
                    if (dto.id == null
                            || dto.namePattern == null
                            || dto.namePattern.isBlank()) {
                        continue;
                    }
                    try {
                        Pattern p = Pattern.compile(dto.namePattern);
                        archetypes.add(
                                new ResolvedArchetype(
                                        dto.id,
                                        p,
                                        dto.preferredTags == null ? List.of() : dto.preferredTags,
                                        dto.bonusPerHit <= 0 ? 2 : dto.bonusPerHit,
                                        dto.maxBonus < 0 ? 0 : dto.maxBonus));
                    } catch (Exception e) {
                        log.warn("Skip invalid archetype {}: {}", dto.id, e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            log.warn("Failed to read business_fit_rules.json: {}", e.getMessage());
        }
    }

    public ResolvedArchetype matchArchetype(String profileName, String fallbackQuery) {
        String concat = safe(profileName) + " " + safe(fallbackQuery);
        for (ResolvedArchetype a : archetypes) {
            if (a.namePattern.matcher(concat).find()) {
                return a;
            }
        }
        return archetypes.isEmpty()
                ? new ResolvedArchetype("generic", Pattern.compile(".*"), List.of(), 2, 10)
                : archetypes.get(archetypes.size() - 1);
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class Wrapper {
        public List<ArchetypeDto> archetypes;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class ArchetypeDto {
        public String id;
        public String namePattern;
        public List<String> preferredTags;
        public double bonusPerHit = 2;
        public double maxBonus = 10;
    }

    public record ResolvedArchetype(
            String id, Pattern namePattern, List<String> preferredTags, double bonusPerHit, double maxBonus) {}
}
