package com.example.backend.batch;

import com.example.backend.entities.Poi;
import com.example.backend.overpass.OsmElement;
import com.example.backend.repositories.PoiRepository;
import java.util.List;
import java.util.Map;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Component
@StepScope
public class PoiItemProcessor implements ItemProcessor<OsmElement, Poi> {

    private final PoiRepository poiRepository;
    private final GeometryFactory geometryFactory;

    public PoiItemProcessor(PoiRepository poiRepository, GeometryFactory geometryFactory) {
        this.poiRepository = poiRepository;
        this.geometryFactory = geometryFactory;
    }

    @Override
    public Poi process(OsmElement element) throws Exception {
        if (poiRepository.existsByOsmId(Long.toString(element.id()))) {
            return null;
        }
        Point location = geometryFactory.createPoint(
            new Coordinate(element.lon(), element.lat())
        );
        Map<String, String> tags = element.tags();
        String name = tags.getOrDefault("name", "Unknown");

        Poi poi = new Poi();
        poi.setOsmId(Long.toString(element.id()));
        poi.setName(name);
        poi.setTypeTag(deriveTypeTag(tags));
        poi.setLocation(location);
        return poi;
    }

    private static String deriveTypeTag(Map<String, String> tags) {
        for (String key : List.of("amenity", "shop", "leisure", "office", "tourism")) {
            String v = tags.get(key);
            if (v != null && !v.isBlank()) {
                return key + "=" + v;
            }
        }
        if (!tags.isEmpty()) {
            var e = tags.entrySet().iterator().next();
            return e.getKey() + "=" + e.getValue();
        }
        return "unknown";
    }
}