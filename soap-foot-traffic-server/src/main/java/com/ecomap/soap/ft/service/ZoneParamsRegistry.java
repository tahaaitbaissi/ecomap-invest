package com.ecomap.soap.ft.service;

import com.ecomap.foottraffic.simulation.FootTrafficZoneParamsSnapshot;
import com.ecomap.soap.ft.entities.FootTrafficZoneParamsEntity;
import com.ecomap.soap.ft.repo.FootTrafficZoneParamsRepository;
import jakarta.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class ZoneParamsRegistry {

    private static final String FALLBACK = "RESIDENTIAL";

    private final FootTrafficZoneParamsRepository repository;

    public ZoneParamsRegistry(FootTrafficZoneParamsRepository repository) {
        this.repository = repository;
    }
    private final Map<String, FootTrafficZoneParamsSnapshot> cache = new ConcurrentHashMap<>();

    @PostConstruct
    void load() {
        for (FootTrafficZoneParamsEntity e : repository.findAll()) {
            cache.put(e.getArchetype().toUpperCase(), toSnapshot(e));
        }
    }

    public FootTrafficZoneParamsSnapshot resolve(String archetype) {
        if (archetype == null) {
            return cache.get(FALLBACK);
        }
        return cache.getOrDefault(archetype.toUpperCase(), cache.get(FALLBACK));
    }

    private static FootTrafficZoneParamsSnapshot toSnapshot(FootTrafficZoneParamsEntity e) {
        return new FootTrafficZoneParamsSnapshot(
                e.getArchetype(),
                e.getBaseDailyMin(),
                e.getBaseDailyMax(),
                e.getPoiDensityCap(),
                e.getPopDensityCap(),
                e.getIncomeWeight(),
                e.getHourlyCurveWd(),
                e.getNoiseSigma());
    }
}
