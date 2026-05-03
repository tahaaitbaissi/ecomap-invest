package com.example.backend.controllers;

import com.example.backend.controllers.dto.PoiMapResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.example.backend.services.PoiService;
import com.example.backend.util.ViewportBbox;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "POI", description = "Points of interest in map viewport")
@RestController
@RequestMapping("/api/v1/poi")
@RequiredArgsConstructor
public class PoiController {

    private final PoiService poiService;

    @Value("${app.poi.max-bbox-deg:0.5}")
    private double maxBboxDeg;

    /**
     * Returns POIs in a map viewport. Query parameters are the west–south and east–north corners
     * (WGS84): {@code minX} = south-west longitude, {@code minY} = south-west latitude, {@code
     * maxX} = north-east longitude, {@code maxY} = north-east latitude — the same order as the
     * former comma-separated {@code swLng,swLat,neLng,neLat} string.
     */
    @Operation(summary = "List POIs in bounding box (minX,minY,maxX,maxY WGS84)")
    @GetMapping
    public ResponseEntity<?> getPoisInViewport(
            @RequestParam("minX") double minX,
            @RequestParam("minY") double minY,
            @RequestParam("maxX") double maxX,
            @RequestParam("maxY") double maxY) {
        try {
            ViewportBbox.validatePoiView(minX, minY, maxX, maxY, maxBboxDeg);
            List<PoiMapResponse> pois = poiService.getPoisInBoundingBox(minX, minY, maxX, maxY);
            return ResponseEntity.ok()
                    .cacheControl(CacheControl.maxAge(5, TimeUnit.MINUTES))
                    .body(pois);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }
}
