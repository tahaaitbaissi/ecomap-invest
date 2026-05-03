package com.example.backend.repositories;

import static org.assertj.core.api.Assertions.assertThat;

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

/** PostGIS spatial queries ({@link PoiRepository#findByBoundingBox}). */
@Tag("integration")
@SpringBootTest
@Transactional
class PoiRepositorySpatialIT extends AbstractPostgisRedisIntegrationTest {

    @Autowired
    private PoiRepository poiRepository;

    @Test
    void findByBoundingBox_returnsPointsInsideEnvelope() {
        var gf = new GeometryFactory(new PrecisionModel(), 4326);
        double lat = 33.55;
        double lng = -7.60;
        Poi p = new Poi();
        p.setOsmId("spatial-it-1");
        p.setName("InsideBox");
        p.setTypeTag("amenity=cafe");
        p.setLocation(gf.createPoint(new Coordinate(lng, lat)));
        poiRepository.save(p);
        poiRepository.flush();

        // Box containing the point (minX,minY,maxX,maxY) = (SW lng/lat, NE lng/lat)
        double minX = -7.65;
        double minY = 33.50;
        double maxX = -7.55;
        double maxY = 33.60;

        var found = poiRepository.findByBoundingBox(minX, minY, maxX, maxY);
        assertThat(found).extracting(Poi::getOsmId).contains("spatial-it-1");
    }
}
