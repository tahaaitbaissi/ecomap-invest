package com.example.backend.batch;

import com.example.backend.entities.Poi;
import com.example.backend.overpass.OsmElement;
import com.example.backend.repositories.PoiRepository;
import org.locationtech.jts.geom.*;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;
import java.util.Random;

@Component
@StepScope
public class PoiItemProcessor implements ItemProcessor<OsmElement, Poi> {

    private final PoiRepository poiRepository;
    private static final Random RANDOM = new Random();

    public PoiItemProcessor(PoiRepository poiRepository) {
        this.poiRepository = poiRepository;
    }

    @Override
    public Poi process(OsmElement element) throws Exception {
        if (poiRepository.existsByOsmId(element.getId().toString())) {
            return null;
        }
        GeometryFactory factory = new GeometryFactory(new PrecisionModel(), 4326);
        Point location = factory.createPoint(
            new Coordinate(element.getLon(), element.getLat())
        );
        String name = element.getTags().getOrDefault("name", "Unknown");
        boolean isHighIncome = isHighIncomeZone(element.getLat(), element.getLon());
        int priceLevel = isHighIncome ? RANDOM.nextInt(2) + 3 : RANDOM.nextInt(3) + 1;
        float rating = 3.0f + RANDOM.nextFloat() * 2.0f;

        return Poi.builder()
            .osmId(element.getId().toString())
            .name(name)
            .typeTag(element.getTypeTag())
            .location(location)
            .priceLevel(priceLevel)
            .rating(rating)
            .build();
    }

    private boolean isHighIncomeZone(double lat, double lon) {
        return (Math.abs(lat - 33.57) < 0.02 && Math.abs(lon + 7.63) < 0.02)
            || (Math.abs(lat - 33.58) < 0.02 && Math.abs(lon + 7.61) < 0.02);
    }
}