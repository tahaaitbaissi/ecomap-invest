package com.example.backend.services.profile;

import com.example.backend.controllers.dto.ProfileTagOptionResponse;
import com.example.backend.controllers.dto.TagWeightDto;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ProfileTagCatalog {

    public record TagOption(
            String tag,
            String label,
            String group,
            String description,
            List<String> aliases
    ) {}

    private static final List<TagOption> OPTIONS = List.of(
            new TagOption(
                    "amenity=cafe",
                    "Cafe",
                    "Food",
                    "Coffee shops and casual meeting places.",
                    List.of("amenity=coffee_shop")),
            new TagOption(
                    "amenity=restaurant",
                    "Restaurant",
                    "Food",
                    "Restaurants and sit-down food businesses.",
                    List.of("amenity=fast_food", "amenity=food_court")),
            new TagOption(
                    "amenity=school",
                    "School",
                    "Education",
                    "Schools that create daily family and staff traffic.",
                    List.of("category=school")),
            new TagOption(
                    "amenity=university",
                    "University",
                    "Education",
                    "Higher-education campuses and student traffic.",
                    List.of("category=university")),
            new TagOption(
                    "amenity=hospital",
                    "Hospital",
                    "Health",
                    "Hospitals and major health anchors.",
                    List.of("amenity=clinic", "amenity=doctors")),
            new TagOption(
                    "amenity=pharmacy",
                    "Pharmacy",
                    "Health",
                    "Retail pharmacies and chemists.",
                    List.of("amenity=chemist")),
            new TagOption(
                    "amenity=bank",
                    "Bank",
                    "Services",
                    "Banks and financial-service anchors.",
                    List.of("shop=financial_services", "amenity=atm")),
            new TagOption(
                    "shop=supermarket",
                    "Supermarket",
                    "Retail",
                    "Large grocery and everyday-shopping anchors.",
                    List.of("shop=convenience", "marketplace=market")),
            new TagOption(
                    "shop=bakery",
                    "Bakery",
                    "Retail",
                    "Bakeries and pastry shops.",
                    List.of("shop=pastry", "shop=patisserie")),
            new TagOption(
                    "shop=clothes",
                    "Clothing store",
                    "Retail",
                    "Fashion and apparel shops.",
                    List.of("shop=fashion")),
            new TagOption(
                    "leisure=park",
                    "Park",
                    "Leisure",
                    "Parks and recreation areas that attract visitors.",
                    List.of()),
            new TagOption(
                    "leisure=gym",
                    "Gym",
                    "Leisure",
                    "Gyms and fitness centers.",
                    List.of("leisure=fitness_centre", "sport=fitness")),
            new TagOption(
                    "office=company",
                    "Company office",
                    "Office",
                    "General offices and employment centers.",
                    List.of("office=lawyer", "office=law_firm", "amenity=lawyer", "amenity=notary")));

    private final Map<String, TagOption> byTag;
    private final Map<String, String> aliasToTag;

    public ProfileTagCatalog() {
        LinkedHashMap<String, TagOption> tags = new LinkedHashMap<>();
        LinkedHashMap<String, String> aliases = new LinkedHashMap<>();
        for (TagOption option : OPTIONS) {
            String canonicalTag = normalize(option.tag());
            tags.put(canonicalTag, option);
            aliases.put(canonicalTag, canonicalTag);
            for (String alias : option.aliases()) {
                aliases.put(normalize(alias), canonicalTag);
            }
        }
        byTag = Map.copyOf(tags);
        aliasToTag = Map.copyOf(aliases);
    }

    public List<ProfileTagOptionResponse> options() {
        return OPTIONS.stream()
                .sorted(Comparator.comparing(TagOption::group).thenComparing(TagOption::label))
                .map(o -> new ProfileTagOptionResponse(o.tag(), o.label(), o.group(), o.description(), o.aliases()))
                .toList();
    }

    public String promptCatalog() {
        return OPTIONS.stream()
                .map(o -> "- " + o.tag() + " (" + o.label() + ", " + o.group() + "): " + o.description())
                .reduce((a, b) -> a + "\n" + b)
                .orElse("");
    }

    public boolean isSupported(String tag) {
        return canonicalTag(tag).isPresent();
    }

    public Optional<String> canonicalTag(String tag) {
        if (tag == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(aliasToTag.get(normalize(tag)));
    }

    public List<TagWeightDto> canonicalize(List<TagWeightDto> tags, String context) {
        if (tags == null || tags.isEmpty()) {
            return List.of();
        }
        List<TagWeightDto> out = tags.stream()
                .filter(t -> t != null && t.tag() != null && !t.tag().trim().isEmpty())
                .flatMap(t -> {
                    Optional<String> canonical = canonicalTag(t.tag());
                    if (canonical.isEmpty()) {
                        log.warn("Ignoring unsupported profile tag from {}: {}", context, t.tag());
                        return java.util.stream.Stream.empty();
                    }
                    return java.util.stream.Stream.of(new TagWeightDto(canonical.get(), t.weight()));
                })
                .toList();
        return FallbackProfileProvider.dedupeByTagKeepMaxWeight(out);
    }

    private static String normalize(String tag) {
        return tag.trim().toLowerCase(Locale.ROOT);
    }
}
