package com.example.backend.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.example.backend.scoring.RawScoreRefBounds;
import org.junit.jupiter.api.Test;

class ProfileScoreScaleServiceTest {

    @Test
    void heatmap_nonFlat_viewportTwoCells_stableScale() {
        RawScoreRefBounds ref = new RawScoreRefBounds(10, 110);
        assertEquals(0.0, ProfileScoreScaleService.toHeatmapDisplayScore(10, ref, 2), 0.001);
        assertEquals(100.0, ProfileScoreScaleService.toHeatmapDisplayScore(110, ref, 2), 0.001);
        assertEquals(35.0, ProfileScoreScaleService.toHeatmapDisplayScore(45, ref, 10), 0.001);
    }

    @Test
    void heatmap_globalFlat_twoCells_returnsNull() {
        RawScoreRefBounds ref = RawScoreRefBounds.degenerate(5);
        assertNull(ProfileScoreScaleService.toHeatmapDisplayScore(5, ref, 2));
    }

    @Test
    void heatmap_globalFlat_single_returns50() {
        RawScoreRefBounds ref = RawScoreRefBounds.degenerate(5);
        assertEquals(50.0, ProfileScoreScaleService.toHeatmapDisplayScore(5, ref, 1), 0.001);
    }

    @Test
    void sim_clampsToHundred() {
        RawScoreRefBounds ref = new RawScoreRefBounds(0, 100);
        assertEquals(100.0, ProfileScoreScaleService.toSimAdjustedDisplay(200, ref), 0.001);
        assertEquals(0.0, ProfileScoreScaleService.toSimAdjustedDisplay(-50, ref), 0.001);
    }

    @Test
    void sim_degenerateRef_usesFiftyPlusDelta() {
        RawScoreRefBounds ref = RawScoreRefBounds.degenerate(0);
        assertEquals(55.0, ProfileScoreScaleService.toSimAdjustedDisplay(5, ref), 0.001);
    }

    @Test
    void percentileLinear_interpolatesMidpoint() {
        double[] sorted = new double[] {0, 10, 20, 30, 40};
        assertEquals(10.0, ProfileScoreScaleService.percentileLinear(sorted, 25.0), 0.001);
        assertEquals(20.0, ProfileScoreScaleService.percentileLinear(sorted, 50.0), 0.001);
    }
}
