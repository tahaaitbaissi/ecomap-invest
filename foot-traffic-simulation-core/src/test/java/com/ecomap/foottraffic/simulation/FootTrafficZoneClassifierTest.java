package com.ecomap.foottraffic.simulation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class FootTrafficZoneClassifierTest {

    @Test
    void classify_transitHub_priority() {
        var c = new PoiTagCounts(
                3,
                0,
                100, 0, 0,
                0, 0, 0, 0,
                0, 0, 0,
                0, 0,
                0,
                0,
                0,
                103);

        var r = FootTrafficZoneClassifier.classify(c, 10_000);
        assertThat(r.archetype()).isEqualTo("TRANSIT_HUB");
        assertThat(r.confidence()).isBetween(0.0, 1.0);
    }

    @Test
    void classify_cbd_requires_density_and_drivers() {
        var c = new PoiTagCounts(
                0, 0,
                6, 1, 1,
                0, 0, 0, 0,
                0, 0, 0,
                0, 0,
                0,
                0,
                0,
                8);

        var r = FootTrafficZoneClassifier.classify(c, 6_000);
        assertThat(r.archetype()).isEqualTo("CBD");
        assertThat(r.confidence()).isEqualTo(1.0);
    }

    @Test
    void classify_rural_low_counts() {
        var c = new PoiTagCounts(
                0, 0,
                0, 0, 0,
                0, 0, 0, 0,
                0, 0, 0,
                0, 0,
                0,
                0,
                0,
                0);

        var r = FootTrafficZoneClassifier.classify(c, 0);
        assertThat(r.archetype()).isEqualTo("RURAL");
        assertThat(r.confidence()).isEqualTo(0.0);
    }

    @Test
    void classify_sparse_one_poi_high_pop_is_residential() {
        var c = new PoiTagCounts(
                0, 0,
                0, 0, 0,
                0, 0, 0, 0,
                0, 0, 0,
                0, 0,
                0,
                0,
                0,
                1);

        var r = FootTrafficZoneClassifier.classify(c, 3000);
        assertThat(r.archetype()).isEqualTo("RESIDENTIAL");
    }

    @Test
    void normalizeArchetype_defaults_and_normalizes() {
        assertThat(FootTrafficZoneClassifier.normalizeArchetype(null)).isEqualTo("RESIDENTIAL");
        assertThat(FootTrafficZoneClassifier.normalizeArchetype("  campus ")).isEqualTo("CAMPUS");
    }
}
