package com.example.backend.foottraffic.simulation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class FootTrafficZoneClassifierTest {

    @Test
    void classify_transitHub_priority() {
        var c = new PoiTagCounts(
                3, // transitPoiCount
                0, // railwayStationCount
                100, 0, 0, // office/bank/hotel (would trigger CBD/OFFICE)
                0, 0, 0, 0, // shop/rest/cafe/bar
                0, 0, 0, // school/university/kindergarten
                0, 0, // park/nature
                0, // residential
                0, // competitorApprox
                103 // total
        );

        var r = FootTrafficZoneClassifier.classify(c, 10_000);
        assertThat(r.archetype()).isEqualTo("TRANSIT_HUB");
        assertThat(r.confidence()).isBetween(0.0, 1.0);
    }

    @Test
    void classify_cbd_requires_density_and_drivers() {
        var c = new PoiTagCounts(
                0, 0,
                6, 1, 1, // office/bank/hotel => cbdDrivers=8
                0, 0, 0, 0,
                0, 0, 0,
                0, 0,
                0,
                0,
                8
        );

        var r = FootTrafficZoneClassifier.classify(c, 6_000);
        assertThat(r.archetype()).isEqualTo("CBD");
        assertThat(r.confidence()).isEqualTo(1.0);
    }

    @Test
    void classify_office() {
        var c = new PoiTagCounts(
                0, 0,
                5, 0, 0,
                0, 0, 0, 0,
                0, 0, 0,
                0, 0,
                0,
                0,
                5
        );

        var r = FootTrafficZoneClassifier.classify(c, 100);
        assertThat(r.archetype()).isEqualTo("OFFICE");
        assertThat(r.confidence()).isEqualTo(1.0);
    }

    @Test
    void classify_campus() {
        var c = new PoiTagCounts(
                0, 0,
                0, 0, 0,
                0, 0, 0, 0,
                2, 1, 1, // education total 4
                0, 0,
                0,
                0,
                4
        );

        var r = FootTrafficZoneClassifier.classify(c, 100);
        assertThat(r.archetype()).isEqualTo("CAMPUS");
        assertThat(r.confidence()).isEqualTo(1.0);
    }

    @Test
    void classify_retail() {
        var c = new PoiTagCounts(
                0, 0,
                0, 0, 0,
                7, 2, 1, 0, // retailFood total 10
                0, 0, 0,
                0, 0,
                0,
                0,
                10
        );

        var r = FootTrafficZoneClassifier.classify(c, 100);
        assertThat(r.archetype()).isEqualTo("RETAIL");
        assertThat(r.confidence()).isEqualTo(1.0);
    }

    @Test
    void classify_park_requires_low_nonPark() {
        var c = new PoiTagCounts(
                0, 0,
                0, 0, 0,
                0, 0, 0, 0,
                0, 0, 0,
                2, 0, // parkNature=2
                0,
                0,
                2
        );

        var r = FootTrafficZoneClassifier.classify(c, 100);
        assertThat(r.archetype()).isEqualTo("RURAL"); // total < 3 short-circuits before park
        assertThat(r.confidence()).isBetween(0.0, 1.0);
    }

    @Test
    void classify_park_when_not_rural() {
        var c = new PoiTagCounts(
                0, 0,
                0, 0, 0,
                1, 0, 0, 0, // nonPark=1
                0, 0, 0,
                2, 0, // parkNature=2, nonPark < 5
                0,
                0,
                3
        );

        var r = FootTrafficZoneClassifier.classify(c, 100);
        assertThat(r.archetype()).isEqualTo("PARK");
        assertThat(r.confidence()).isEqualTo(2.0 / 3.0);
    }

    @Test
    void classify_residential_by_density() {
        var c = new PoiTagCounts(
                0, 0,
                0, 0, 0,
                0, 0, 0, 0,
                0, 0, 0,
                0, 0,
                0,
                0,
                3
        );

        var r = FootTrafficZoneClassifier.classify(c, 1500);
        assertThat(r.archetype()).isEqualTo("RESIDENTIAL");
        assertThat(r.confidence()).isEqualTo(1.0 / 3.0);
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
                0
        );

        var r = FootTrafficZoneClassifier.classify(c, 0);
        assertThat(r.archetype()).isEqualTo("RURAL");
        assertThat(r.confidence()).isEqualTo(0.0);
    }

    @Test
    void normalizeArchetype_defaults_and_normalizes() {
        assertThat(FootTrafficZoneClassifier.normalizeArchetype(null)).isEqualTo("RESIDENTIAL");
        assertThat(FootTrafficZoneClassifier.normalizeArchetype("  campus ")).isEqualTo("CAMPUS");
    }
}

