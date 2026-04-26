package com.example.backend.batch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import com.example.backend.entities.Poi;
import com.example.backend.overpass.OsmElement;
import com.example.backend.repositories.PoiRepository;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PoiItemProcessorTest {

    @Mock
    private PoiRepository poiRepository;

    private static GeometryFactory gf() {
        return new GeometryFactory(new PrecisionModel(), 4326);
    }

    @Test
    void process_duplicate_returnsNull() throws Exception {
        PoiItemProcessor processor = new PoiItemProcessor(poiRepository, gf());
        when(poiRepository.existsByOsmId("99")).thenReturn(true);
        var el = new OsmElement(99L, 33.0, -7.0, Map.of("name", "Cafe", "amenity", "cafe"));
        assertNull(processor.process(el));
    }

    @Test
    void process_new_buildsPoi() throws Exception {
        PoiItemProcessor processor = new PoiItemProcessor(poiRepository, gf());
        when(poiRepository.existsByOsmId("1")).thenReturn(false);
        var el = new OsmElement(1L, 33.0, -7.0, Map.of("name", "Cafe", "amenity", "cafe"));
        Poi p = processor.process(el);
        assertEquals("1", p.getOsmId());
        assertEquals("Cafe", p.getName());
        assertEquals("amenity=cafe", p.getTypeTag());
    }
}
