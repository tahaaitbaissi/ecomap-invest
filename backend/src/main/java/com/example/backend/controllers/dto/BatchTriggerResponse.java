package com.example.backend.controllers.dto;

public record BatchTriggerResponse(long jobInstanceId, long jobExecutionId, String status) {}
