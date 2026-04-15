package com.example.backend.services;

import com.example.backend.controllers.dto.PoiMapResponse;
import com.example.backend.entities.Poi;
import com.example.backend.repositories.PoiRepository;
import com.example.backend.scoring.ScoringStrategy;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for POI operations: retrieval, proximity analysis, and saturation scoring via {@link ScoringStrategy}
 * (local formula vs distributed RMI, selected by {@code app.scoring.strategy}).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PoiService {

    private final PoiRepository poiRepository;
    private final ScoringStrategy scoringStrategy;

    /**
     * Driver categories (POI types that positively influence investment potential).
     * These represent anchor tenants or infrastructure that attracts visitors.
     */
    @Value("${app.poi.driver-categories:category=school,category=university,category=office}")
    private String driverCategoriesConfig;

    /**
     * Radius in kilometers for searching driver POIs (positive influence).
     */
    @Value("${app.poi.radius-drivers-km:2.0}")
    private double radiusDriversKm;

    /**
     * Radius in kilometers for searching competitor POIs (negative influence).
     */
    @Value("${app.poi.radius-competitors-km:1.0}")
    private double radiusCompetitorsKm;

    /**
     * Maximum POI count in density calculation (used to normalize density to [0, 1]).
     */
    @Value("${app.poi.max-density-count:50}")
    private int maxDensityCount;

    private volatile Set<String> cachedDriverCategories;

    @Transactional(readOnly = true)
    public List<PoiMapResponse> getPoisInBoundingBox(double swLng, double swLat, double neLng, double neLat) {
        List<Poi> pois = poiRepository.findAllInBoundingBox(swLng, swLat, neLng, neLat);

        // Compute saturation scores concurrently for all POIs
        List<CompletableFuture<PoiMapResponse>> futures = pois.stream()
            .map(this::toResponseWithScoring)
            .toList();

        // Wait for all async scoring operations
        return futures.stream()
            .map(CompletableFuture::join)
            .toList();
    }

    /**
     * Convert POI to response DTO with saturation score (computed remotely via RMI).
     * Handles RMI failures gracefully by returning null score if service is unavailable.
     */
    private CompletableFuture<PoiMapResponse> toResponseWithScoring(Poi poi) {
        try {
            return computeSaturationScore(poi)
                .handle((score, ex) -> {
                    if (ex != null) {
                        log.warn("Failed to compute saturation score for POI {}: {}", poi.getId(), ex.getMessage());
                        return toResponse(poi, null);  // No score on failure
                    }
                    return toResponse(poi, score);
                });
        } catch (Exception e) {
            log.warn("Error initiating saturation scoring for POI {}: {}", poi.getId(), e.getMessage());
            return CompletableFuture.completedFuture(toResponse(poi, null));
        }
    }

    /**
     * Compute saturation score for a POI by:
     * 1. Counting drivers (positive influence) within driver radius
     * 2. Counting competitors (negative influence) within competitor radius
     * 3. Estimating density (population/traffic) around the POI
     * 4. Calling RMI scoring service with these parameters
     *
     * @param poi the POI to score
     * @return CompletableFuture with saturation score [0-100]
     */
    private CompletableFuture<Double> computeSaturationScore(Poi poi) {
        try {
            double lat = poi.getLocation().getY();
            double lng = poi.getLocation().getX();
            int drivers = countDriversNear(lat, lng);
            int competitors = countCompetitorsNearForPoi(lat, lng, poi.getTypeTag());
            double density = estimateDensity(lat, lng);

            log.debug("Computing saturation score for POI {} ({}): drivers={}, competitors={}, density={}",
                poi.getId(), poi.getName(), drivers, competitors, density);

            return scoringStrategy.computeSaturationScore(drivers, competitors, density);
        } catch (Exception e) {
            log.error("Exception during saturation score computation setup for POI {}: {}",
                poi.getId(), e.getMessage(), e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Count "driver" POIs (positive indicators) within the configured driver radius.
     * Drivers include schools, universities, offices, etc.
     */
    private int countDriversNear(double lat, double lng) {
        Set<String> drivers = getDriverCategories();
        long total = 0;

        for (String driverCategory : drivers) {
            long count = poiRepository.countByTypeTagAndNearby(
                driverCategory,
                lat,
                lng,
                radiusDriversKm * 1000  // Convert km to meters
            );
            total += count;
        }

        return (int) Math.min(total, Integer.MAX_VALUE);
    }

    /**
     * Competitors for an existing POI row: same type_tag count minus the focal feature.
     */
    private int countCompetitorsNearForPoi(double lat, double lng, String typeTag) {
        long count = poiRepository.countByTypeTagAndNearby(
            typeTag,
            lat,
            lng,
            radiusCompetitorsKm * 1000  // Convert km to meters
        );
        return (int) Math.min(Math.max(count - 1, 0), Integer.MAX_VALUE);  // Exclude the POI itself
    }

    /**
     * Estimate population/traffic density around the POI.
     * Currently estimates based on the count of all nearby POIs (proxy for density).
     * Returns a normalized score [0.0, 1.0], where 1.0 = max density.
     *
     * In a production system, this could integrate real census/traffic data for more accuracy.
     */
    private double estimateDensity(double lat, double lng) {
        long nearbyPoiCount = poiRepository.countAllNearby(
            lat,
            lng,
            radiusDriversKm * 1000  // Use driver radius for density estimation
        );

        // Normalize: divide by max expected count, clamp to [0, 1]
        double density = (double) nearbyPoiCount / maxDensityCount;
        return Math.min(Math.max(density, 0.0), 1.0);
    }

    /**
     * Parse driver categories from configuration string (comma-separated).
     * Example: "category=school,category=university,category=office"
     */
    private Set<String> getDriverCategories() {
        if (cachedDriverCategories != null) {
            return cachedDriverCategories;
        }
        synchronized (this) {
            if (cachedDriverCategories == null) {
                cachedDriverCategories = Arrays.stream(driverCategoriesConfig.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .toList()
                    .stream()
                    .collect(java.util.stream.Collectors.toSet());
                log.debug("Loaded driver categories: {}", cachedDriverCategories);
            }
            return cachedDriverCategories;
        }
    }

    /**
     * Convert Poi entity to PoiMapResponse DTO.
     */
    private PoiMapResponse toResponse(Poi poi, Double saturationScore) {
        return new PoiMapResponse(
            poi.getId(),
            poi.getName(),
            poi.getAddress(),
            poi.getTypeTag(),
            poi.getLocation().getY(),
            poi.getLocation().getX(),
            saturationScore
        );
    }
}
