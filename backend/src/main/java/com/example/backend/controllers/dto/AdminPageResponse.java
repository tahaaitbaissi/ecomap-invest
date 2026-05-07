package com.example.backend.controllers.dto;

import java.util.List;

public record AdminPageResponse<T>(
        List<T> items,
        long totalElements,
        int page,
        int size) {}

