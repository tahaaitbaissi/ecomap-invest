package com.example.backend.services.profile;

import com.example.backend.controllers.dto.TagWeightDto;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Deterministic, hardcoded profiles used as an MVP profile generator.
 *
 * <p>Outputs driver/competitor tag+weight lists compatible with
 * {@code HexagonScoringService.parseTagWeights(JsonNode)}.
 */
@Component
public class FallbackProfileProvider {

    public record ProfileConfig(List<TagWeightDto> drivers, List<TagWeightDto> competitors) {}

    private static final ProfileConfig DEFAULT_PROFILE =
            new ProfileConfig(
                    List.of(
                            new TagWeightDto("amenity=school", 0.9),
                            new TagWeightDto("amenity=university", 1.0),
                            new TagWeightDto("office=company", 1.0),
                            new TagWeightDto("shop=supermarket", 0.8),
                            new TagWeightDto("leisure=park", 0.5)),
                    List.of(
                            new TagWeightDto("amenity=restaurant", 1.0),
                            new TagWeightDto("amenity=cafe", 0.9)));

    private final Map<String, Set<String>> keywordsByProfile = new LinkedHashMap<>();

    public FallbackProfileProvider() {
        keywordsByProfile.put("cafe", Set.of("cafe", "coffee", "cafeteria", "قهوة", "قهوا"));
        keywordsByProfile.put("restaurant", Set.of("restaurant", "resto", "snack", "diner", "مطعم"));
        keywordsByProfile.put("boulangerie", Set.of("boulangerie", "bakery", "patisserie", "pâtisserie", "خبز", "مخبزة"));
        keywordsByProfile.put("pharmacie", Set.of("pharmacie", "pharmacy", "drugstore", "صيدلية"));
        keywordsByProfile.put("gym", Set.of("gym", "fitness", "salle", "sport", "musculation", "نادي"));
        keywordsByProfile.put("supermarche", Set.of("supermarche", "supermarché", "supermarket", "market", "marjane", "carrefour"));
    }

    public ProfileConfig findBestMatch(String userQuery) {
        String q = normalize(userQuery);
        if (q.isBlank()) {
            return DEFAULT_PROFILE;
        }

        String bestKey = null;
        int bestScore = 0;
        for (Map.Entry<String, Set<String>> e : keywordsByProfile.entrySet()) {
            int score = 0;
            for (String kw : e.getValue()) {
                if (q.contains(normalize(kw))) {
                    score++;
                }
            }
            if (score > bestScore) {
                bestScore = score;
                bestKey = e.getKey();
            }
        }

        if (bestKey == null || bestScore == 0) {
            return DEFAULT_PROFILE;
        }

        return profileForKey(bestKey);
    }

    private static ProfileConfig profileForKey(String key) {
        return switch (key) {
            case "cafe" -> new ProfileConfig(
                    List.of(
                            new TagWeightDto("amenity=school", 0.7),
                            new TagWeightDto("office=company", 1.0),
                            new TagWeightDto("amenity=university", 0.9),
                            new TagWeightDto("public_transport=station", 0.8),
                            new TagWeightDto("leisure=park", 0.6)),
                    List.of(
                            new TagWeightDto("amenity=cafe", 1.2),
                            new TagWeightDto("amenity=restaurant", 0.7)));
            case "restaurant" -> new ProfileConfig(
                    List.of(
                            new TagWeightDto("office=company", 1.0),
                            new TagWeightDto("amenity=university", 0.9),
                            new TagWeightDto("shop=supermarket", 0.6),
                            new TagWeightDto("leisure=park", 0.5)),
                    List.of(
                            new TagWeightDto("amenity=restaurant", 1.2),
                            new TagWeightDto("amenity=fast_food", 0.9)));
            case "boulangerie" -> new ProfileConfig(
                    List.of(
                            new TagWeightDto("shop=supermarket", 0.8),
                            new TagWeightDto("amenity=school", 0.7),
                            new TagWeightDto("office=company", 0.9),
                            new TagWeightDto("amenity=bank", 0.4)),
                    List.of(
                            new TagWeightDto("shop=bakery", 1.2),
                            new TagWeightDto("shop=convenience", 0.7)));
            case "pharmacie" -> new ProfileConfig(
                    List.of(
                            new TagWeightDto("amenity=hospital", 1.2),
                            new TagWeightDto("amenity=clinic", 1.0),
                            new TagWeightDto("amenity=doctors", 1.0),
                            new TagWeightDto("amenity=school", 0.4)),
                    List.of(
                            new TagWeightDto("amenity=pharmacy", 1.2)));
            case "gym" -> new ProfileConfig(
                    List.of(
                            new TagWeightDto("office=company", 0.9),
                            new TagWeightDto("amenity=university", 0.7),
                            new TagWeightDto("leisure=park", 0.6),
                            new TagWeightDto("shop=supermarket", 0.4)),
                    List.of(
                            new TagWeightDto("leisure=fitness_centre", 1.2),
                            new TagWeightDto("leisure=gym", 1.0)));
            case "supermarche" -> new ProfileConfig(
                    List.of(
                            new TagWeightDto("amenity=school", 0.6),
                            new TagWeightDto("office=company", 0.9),
                            new TagWeightDto("amenity=bank", 0.6),
                            new TagWeightDto("public_transport=station", 0.7)),
                    List.of(
                            new TagWeightDto("shop=supermarket", 1.2),
                            new TagWeightDto("shop=convenience", 0.8)));
            default -> DEFAULT_PROFILE;
        };
    }

    static String normalize(String s) {
        if (s == null) {
            return "";
        }
        String t = s.trim().toLowerCase(Locale.ROOT);
        t = Normalizer.normalize(t, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        t = t.replaceAll("\\s+", " ");
        return t;
    }

    static List<TagWeightDto> dedupeByTagKeepMaxWeight(List<TagWeightDto> in) {
        Map<String, Double> m = new LinkedHashMap<>();
        for (TagWeightDto tw : in) {
            if (tw == null || tw.tag() == null) {
                continue;
            }
            String tag = tw.tag().trim();
            if (tag.isEmpty()) {
                continue;
            }
            double w = tw.weight() == null ? 1.0 : tw.weight();
            m.merge(tag, w, Math::max);
        }
        List<TagWeightDto> out = new ArrayList<>();
        for (Map.Entry<String, Double> e : m.entrySet()) {
            out.add(new TagWeightDto(e.getKey(), e.getValue()));
        }
        return out;
    }
}

