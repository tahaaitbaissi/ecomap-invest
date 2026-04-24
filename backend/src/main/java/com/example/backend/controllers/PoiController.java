package com.example.backend.controllers;

import com.example.backend.controllers.dto.PoiMapResponse;
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

@RestController
@RequestMapping("/api/v1/poi")
@RequiredArgsConstructor
public class PoiController {

    private final PoiService poiService;

    @Value("${app.poi.max-bbox-deg:0.5}")
    private double maxBboxDeg;

    @GetMapping
    public ResponseEntity<?> getPoisInViewport(@RequestParam("bbox") String bbox) {
        try {
            String[] parts = bbox.split(",");
            if (parts.length != 4) {
                return ResponseEntity.badRequest().body("bbox must contain 4 comma-separated values: swLng,swLat,neLng,neLat");
            }

            double swLng = Double.parseDouble(parts[0]);
            double swLat = Double.parseDouble(parts[1]);
            double neLng = Double.parseDouble(parts[2]);
            double neLat = Double.parseDouble(parts[3]);
            ViewportBbox.validatePoiView(swLng, swLat, neLng, neLat, maxBboxDeg);

            List<PoiMapResponse> pois = poiService.getPoisInBoundingBox(swLng, swLat, neLng, neLat);
            return ResponseEntity.ok()
                    .cacheControl(CacheControl.maxAge(5, TimeUnit.MINUTES))
                    .body(pois);
        } catch (NumberFormatException ex) {
            return ResponseEntity.badRequest().body("bbox contains invalid numeric values");
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }
}
