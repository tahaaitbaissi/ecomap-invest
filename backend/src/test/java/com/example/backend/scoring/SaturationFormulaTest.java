package com.example.backend.scoring;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SaturationFormulaTest {

    @Test
    void matches_rmi_node_reference_case() {
        // ScoringRemoteImpl: drivers=2, competitors=1, density=0.8 -> raw = 1 + 24 - 1.2 = 23.8
        assertEquals(23.8, SaturationFormula.apply(2, 1, 0.8), 1e-9);
    }

    @Test
    void clamps_density_and_score_bounds() {
        assertEquals(100.0, SaturationFormula.apply(1000, 0, 2.0), 1e-9);
        assertEquals(0.0, SaturationFormula.apply(0, 1000, 0.0), 1e-9);
    }
}
