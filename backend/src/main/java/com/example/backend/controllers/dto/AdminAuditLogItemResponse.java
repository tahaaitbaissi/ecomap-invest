package com.example.backend.controllers.dto;

import java.util.UUID;

public record AdminAuditLogItemResponse(
        long id,
        UUID auditId,
        UUID userId,
        String occurredAt,
        String userEmail,
        String action,
        String method,
        String argsSummary,
        Long durationMs,
        Boolean success,
        String errorClass,
        String errorMessage) {}

