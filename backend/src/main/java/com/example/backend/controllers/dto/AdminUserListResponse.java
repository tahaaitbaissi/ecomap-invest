package com.example.backend.controllers.dto;

import java.util.List;

public record AdminUserListResponse(
        List<AdminUserListItemResponse> items,
        long totalElements,
        int page,
        int size) {}

