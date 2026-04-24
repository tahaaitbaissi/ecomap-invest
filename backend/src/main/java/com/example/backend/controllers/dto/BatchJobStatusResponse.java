package com.example.backend.controllers.dto;

import java.time.LocalDateTime;

public record BatchJobStatusResponse(
        String status,
        LocalDateTime startTime,
        LocalDateTime endTime,
        long readCount,
        long writeCount,
        long skipCount) {}
