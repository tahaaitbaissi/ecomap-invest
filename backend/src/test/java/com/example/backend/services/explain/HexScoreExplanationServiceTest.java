package com.example.backend.services.explain;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.backend.controllers.dto.HexExplanationContextDto;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class HexScoreExplanationServiceTest {

    @Test
    void factsPrompt_includesNormalizationAndTags() {
        UUID id = UUID.randomUUID();
        var dto =
                new HexExplanationContextDto(
                        id,
                        "Café",
                        "Snack",
                        "89283082803ffff",
                        9,
                        false,
                        9,
                        1,
                        "note",
                        33.5,
                        -7.6,
                        12.34,
                        5.5,
                        90.5,
                        false,
                        71.5,
                        0.06,
                        10.5,
                        3.25,
                        List.of(new HexExplanationContextDto.TagContributionRow("amenity=cafe", 1.5, 2, 3.0)),
                        List.of(new HexExplanationContextDto.TagContributionRow("amenity=restaurant", 1.2, 1, 1.2)),
                        new HexExplanationContextDto.DemographicsSnapshot(true, 20000.0, 0.06),
                        15L,
                        12000.5,
                        18000.0,
                        null);

        String facts = HexScoreExplanationService.factsPrompt(dto);

        assertThat(facts).contains("normalizationStretchLow", "normalizationStretchHigh");
        assertThat(facts).contains("amenity=cafe").contains("countAcrossLeaves\": 2");
        assertThat(facts).contains("averageRawAcrossLeaves");
    }

    @Test
    void deterministicNarrative_mentionsDisplayScoreWhenPresent() {
        UUID id = UUID.randomUUID();
        var dto =
                new HexExplanationContextDto(
                        id,
                        "P",
                        "Q",
                        "89283082803ffff",
                        9,
                        false,
                        9,
                        1,
                        "single",
                        0,
                        0,
                        10,
                        0,
                        20,
                        false,
                        73.9,
                        0.07,
                        11,
                        4,
                        List.of(),
                        List.of(),
                        new HexExplanationContextDto.DemographicsSnapshot(false, null, 0.0),
                        0L,
                        null,
                        null,
                        null);

        String out = HexScoreExplanationService.deterministicNarrative(dto);
        assertThat(out).containsIgnoringCase("74");
    }
}
