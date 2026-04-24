package com.example.ecomap.rmi.scoring.server;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ScoringRemoteImplTest {

    @Test
    void ping_returnsMessage() throws Exception {
        var impl = new ScoringRemoteImpl(0);
        assertEquals("EcoMap RMI scoring node OK", impl.ping());
    }

    @Test
    void compute_clamps() throws Exception {
        var impl = new ScoringRemoteImpl(0);
        assertEquals(0.0, impl.computeSaturationScore(0, 0, 0.0), 1e-6);
        assertEquals(100.0, impl.computeSaturationScore(10_000, 0, 1.0), 1e-6);
    }
}
