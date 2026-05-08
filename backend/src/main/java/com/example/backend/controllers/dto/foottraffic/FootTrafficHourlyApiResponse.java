package com.example.backend.controllers.dto.foottraffic;

import java.util.List;

public record FootTrafficHourlyApiResponse(
        String archetype, int baselineDaily, int peakHourly, List<Double> hourly) {}
