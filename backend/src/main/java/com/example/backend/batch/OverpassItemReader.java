package com.example.backend.batch;

import com.example.backend.overpass.OverpassApiClient;
import com.example.backend.overpass.OsmElement;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemReader;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
@StepScope
public class OverpassItemReader implements ItemReader<OsmElement> {

    private final OverpassApiClient overpassApiClient;
    private final Queue<OsmElement> queue = new LinkedList<>();
    private boolean initialized = false;

    private static final List<String> OSM_CATEGORIES = List.of(
        "amenity=cafe", "amenity=restaurant", "amenity=school",
        "amenity=university", "amenity=hospital", "amenity=bank",
        "shop=supermarket", "shop=bakery", "shop=clothes",
        "leisure=park", "leisure=gym", "office=company"
    );

    public OverpassItemReader(OverpassApiClient overpassApiClient) {
        this.overpassApiClient = overpassApiClient;
    }

    @Override
    public OsmElement read() {
        if (!initialized) {
            for (String tag : OSM_CATEGORIES) {
                List<OsmElement> elements = overpassApiClient
                    .fetchByTagAndBBox(tag, 33.45, -7.70, 33.65, -7.50);
                queue.addAll(elements);
            }
            initialized = true;
        }
        return queue.poll();
    }
}