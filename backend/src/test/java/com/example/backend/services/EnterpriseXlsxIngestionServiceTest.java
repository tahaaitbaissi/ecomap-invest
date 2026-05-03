package com.example.backend.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class EnterpriseXlsxIngestionServiceTest {

    @Test
    void normalizeHeader_stripsAccentsAndCase() {
        assertEquals("categorie", EnterpriseXlsxIngestionService.normalizeHeader("Catégorie"));
        assertEquals("nom", EnterpriseXlsxIngestionService.normalizeHeader("  Nom "));
        assertEquals("longitude", EnterpriseXlsxIngestionService.normalizeHeader("Longitude"));
    }

    @Test
    void buildEnterpriseOsmId_stablePerLocation() {
        String a = EnterpriseXlsxIngestionService.buildEnterpriseOsmId("Cafe", 33.5, -7.6);
        String b = EnterpriseXlsxIngestionService.buildEnterpriseOsmId("Cafe", 33.5, -7.6);
        String c = EnterpriseXlsxIngestionService.buildEnterpriseOsmId("Cafe", 33.51, -7.6);
        assertEquals(a, b);
        assertTrue(a.startsWith("enterprise:"));
        assertTrue(!a.equals(c));
    }
}
