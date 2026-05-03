package com.example.backend.controllers.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "Spring Batch job execution summary")
public record BatchJobStatusResponse(
        String status,
        LocalDateTime startTime,
        LocalDateTime endTime,
        long readCount,
        long writeCount,
        long skipCount) {}
