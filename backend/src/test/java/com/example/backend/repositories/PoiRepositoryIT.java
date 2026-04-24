package com.example.backend.repositories;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.backend.entities.Poi;
import com.example.backend.testsupport.AbstractPostgisRedisIntegrationTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@Tag("integration")
@SpringBootTest
@Transactional
class PoiRepositoryIT extends AbstractPostgisRedisIntegrationTest {

    @Autowired
    private PoiRepository poiRepository;

    @Test
    void findByOsmId() {
        var gf = new GeometryFactory(new PrecisionModel(), 4326);
        Poi p = new Poi();
        p.setOsmId("osm-junit-1");
        p.setName("T");
        p.setTypeTag("t=v");
        p.setLocation(gf.createPoint(new Coordinate(-7.5, 33.5)));
        poiRepository.save(p);
        poiRepository.flush();
        assertTrue(poiRepository.findByOsmId("osm-junit-1").isPresent());
    }
}
