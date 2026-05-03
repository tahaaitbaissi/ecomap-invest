package com.example.backend.controllers.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Ids returned when a batch job is launched")
public record BatchTriggerResponse(long jobInstanceId, long jobExecutionId, String status) {}
