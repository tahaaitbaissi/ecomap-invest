package com.example.backend.batch;

import com.example.backend.entities.Poi;
import com.example.backend.overpass.OsmElement;
import com.example.backend.repositories.PoiRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
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
        poi.setTypeTag(element.sourceTag());
        poi.setLocation(location);
        poi.setImportedAt(new Timestamp(System.currentTimeMillis()));

        int priceLevel = simulatePriceLevel(element.lat(), element.lon());
        poi.setPriceLevel(priceLevel);

        float rating = simulateRating();
        poi.setRating(rating);
        return poi;
    }

    private static int simulatePriceLevel(double lat, double lon) {
        // Base 1-4
        int base = ThreadLocalRandom.current().nextInt(1, 5);
        // Approx “high-income” zones: Anfa / Maarif (very rough bbox)
        boolean highIncome =
                lat >= 33.56 && lat <= 33.61 &&
                lon >= -7.66 && lon <= -7.58;
        int biased = highIncome ? base + 1 : base;
        return Math.min(Math.max(biased, 1), 4);
    }

    private static float simulateRating() {
        double raw = ThreadLocalRandom.current().nextDouble(3.0, 5.0);
        double rounded = BigDecimal.valueOf(raw).setScale(1, RoundingMode.HALF_UP).doubleValue();
        return (float) rounded;
    }
}