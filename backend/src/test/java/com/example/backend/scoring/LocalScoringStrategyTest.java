package com.example.backend.scoring;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class LocalScoringStrategyTest {

    @Test
    void returns_formula_result() throws Exception {
        LocalScoringStrategy s = new LocalScoringStrategy();
        double v = s.computeSaturationScore(2, 1, 0.8).get();
        assertEquals(23.8, v, 1e-9);
    }
}
