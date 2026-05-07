package com.example.backend.controllers.dto;

import java.util.List;

public record AdminPoiSearchResponse(
        List<AdminPoiResponse> items,
        int returned) {}

