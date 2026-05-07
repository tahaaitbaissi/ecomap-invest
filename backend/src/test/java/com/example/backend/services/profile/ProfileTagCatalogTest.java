package com.example.backend.services.profile;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.backend.controllers.dto.TagWeightDto;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProfileTagCatalogTest {

    private final ProfileTagCatalog catalog = new ProfileTagCatalog();

    @Test
    void optionsContainOnlyImportedScoringTags() {
        assertThat(catalog.options())
                .extracting(o -> o.tag())
                .contains(
                        "amenity=cafe",
                        "amenity=restaurant",
                        "amenity=school",
                        "amenity=university",
                        "amenity=hospital",
                        "amenity=bank",
                        "amenity=pharmacy",
                        "shop=supermarket",
                        "shop=bakery",
                        "shop=clothes",
                        "leisure=park",
                        "leisure=gym",
                        "office=company");
    }

    @Test
    void canonicalizeMapsAliasesAndDropsUnsupportedTags() {
        List<TagWeightDto> out = catalog.canonicalize(
                List.of(
                        new TagWeightDto("leisure=fitness_centre", 1.2),
                        new TagWeightDto("amenity=notary", 0.8),
                        new TagWeightDto("unsupported=value", 1.0)),
                "test");

        assertThat(out)
                .extracting(TagWeightDto::tag)
                .containsExactly("leisure=gym", "office=company");
    }

    @Test
    void canonicalizeMapsChemistToPharmacy() {
        List<TagWeightDto> out =
                catalog.canonicalize(List.of(new TagWeightDto("amenity=chemist", 1.0)), "test");
        assertThat(out).extracting(TagWeightDto::tag).containsExactly("amenity=pharmacy");
    }
}
