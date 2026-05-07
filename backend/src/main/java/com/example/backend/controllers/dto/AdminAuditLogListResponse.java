package com.example.backend.controllers.dto;

import java.util.List;

public record AdminAuditLogListResponse(
        List<AdminAuditLogItemResponse> items,
        long totalElements,
        int page,
        int size) {}

