package com.example.backend.services.opportunity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class BusinessFitCatalogTest {

    @Test
    void lawyerProfile_matchesLawyerArchetype() {
        BusinessFitCatalog catalog = new BusinessFitCatalog(new ObjectMapper());
        var arch = catalog.matchArchetype("Cabinet Avocat", "services juridiques");
        assertEquals("lawyer", arch.id());
    }

    @Test
    void unknownName_fallsBackToGenericLastRule() {
        BusinessFitCatalog catalog = new BusinessFitCatalog(new ObjectMapper());
        var arch = catalog.matchArchetype("zzz unknown x", "");
        assertEquals("generic", arch.id());
    }
}
