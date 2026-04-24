package com.example.backend.batch;

import com.example.backend.entities.Poi;
import com.example.backend.overpass.OsmElement;
import com.example.backend.repositories.PoiRepository;
import org.locationtech.jts.geom.*;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;

@Component
@StepScope
public class PoiItemProcessor implements ItemProcessor<OsmElement, Poi> {

    private final PoiRepository poiRepository;
    public PoiItemProcessor(PoiRepository poiRepository) {
        this.poiRepository = poiRepository;
    }

    @Override
    public Poi process(OsmElement element) throws Exception {
        if (poiRepository.existsByOsmId(Long.toString(element.id()))) {
            return null;
        }
        GeometryFactory factory = new GeometryFactory(new PrecisionModel(), 4326);
        Point location = factory.createPoint(
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