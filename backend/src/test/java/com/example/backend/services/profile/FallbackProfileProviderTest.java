package com.example.backend.services.profile;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.backend.controllers.dto.TagWeightDto;
import org.junit.jupiter.api.Test;

class FallbackProfileProviderTest {

    @Test
    void cafeQuery_matchesCafeProfile() {
        FallbackProfileProvider p = new FallbackProfileProvider();
        var cfg = p.findBestMatch("Je veux ouvrir un café à Casablanca");
        assertFalse(cfg.drivers().isEmpty());
        assertFalse(cfg.competitors().isEmpty());
        assertTrue(cfg.competitors().stream().map(TagWeightDto::tag).anyMatch(t -> t.equals("amenity=cafe")));
    }

    @Test
    void emptyQuery_returnsDefaultProfile() {
        FallbackProfileProvider p = new FallbackProfileProvider();
        var cfg = p.findBestMatch("   ");
        assertFalse(cfg.drivers().isEmpty());
        assertFalse(cfg.competitors().isEmpty());
    }
}

