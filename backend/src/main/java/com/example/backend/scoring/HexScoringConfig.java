package com.example.backend.scoring;

import java.util.List;

/**
 * Effective driver/competitor tag lists and demographics gate — same semantics as viewport hex scoring.
 */
public record HexScoringConfig(
        List<TagWeight> driverTags, List<TagWeight> competitorTags, boolean useDemographics) {}
