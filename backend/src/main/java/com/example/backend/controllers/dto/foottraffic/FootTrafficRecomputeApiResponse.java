package com.example.backend.controllers.dto.foottraffic;

public record FootTrafficRecomputeApiResponse(int cellsProcessed, long durationMs, long trafficVersion) {}
