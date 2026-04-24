package com.example.backend.batch;

import com.example.backend.overpass.BoundingBox;
import com.example.backend.overpass.OverpassApiClient;
import com.example.backend.overpass.OsmElement;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
@StepScope
public class OverpassItemReader implements ItemReader<OsmElement> {

    private final OverpassApiClient overpassApiClient;
    private final Queue<OsmElement> queue = new LinkedList<>();
    private boolean initialized = false;

    private static final BoundingBox CASABLANCA_BBOX = new BoundingBox(33.45, -7.70, 33.65, -7.50);

    private static final List<String> OSM_CATEGORIES = List.of(
        "amenity=cafe", "amenity=restaurant", "amenity=school",
        "amenity=university", "amenity=hospital", "amenity=bank",
        "shop=supermarket", "shop=bakery", "shop=clothes",
        "leisure=park", "leisure=gym", "office=company"
    );

    public OverpassItemReader(OverpassApiClient overpassApiClient) {
        this.overpassApiClient = overpassApiClient;
    }

    private static void pauseBetweenOverpassCalls() {
        try {
            Thread.sleep(400);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public OsmElement read() {
        if (!initialized) {
            for (int i = 0; i < OSM_CATEGORIES.size(); i++) {
                String tag = OSM_CATEGORIES.get(i);
                List<OsmElement> elements = overpassApiClient
                    .fetchByTagAndBBox(tag, CASABLANCA_BBOX);
                queue.addAll(elements);
                if (i < OSM_CATEGORIES.size() - 1) {
                    pauseBetweenOverpassCalls();
                }
            }
            initialized = true;
        }
        return queue.poll();
    }
}