package com.example.backend.controllers;

import com.example.backend.controllers.dto.foottraffic.FootTrafficHourlyApiResponse;
import com.example.backend.foottraffic.services.FootTrafficService;
import com.example.backend.foottraffic.simulation.DayType;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Foot traffic")
@RestController
@RequestMapping("/api/v1/foot-traffic")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('INVESTOR','ADMIN')")
public class FootTrafficPublicController {

    private final FootTrafficService footTrafficService;

    @GetMapping("/hourly/{h3Index}")
    public ResponseEntity<?> hourly(
            @PathVariable("h3Index") String h3Index,
            @RequestParam(value = "day", defaultValue = "WD") String day,
            @RequestParam(value = "month", defaultValue = "5") int month) {
        DayType dt;
        try {
            dt = DayType.valueOf(day.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("day must be WD, SAT, or SUN");
        }
        var prof = footTrafficService.getProfile(h3Index);
        if (prof.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        var hourlyOpt = footTrafficService.getHourly(h3Index, dt, month);
        if (hourlyOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        double[] h = hourlyOpt.get();
        List<Double> list =
                Arrays.stream(h).boxed().toList();
        return ResponseEntity.ok(
                new FootTrafficHourlyApiResponse(
                        prof.get().getArchetype(),
                        prof.get().getBaselineDaily(),
                        prof.get().getPeakHourly(),
                        list));
    }
}
