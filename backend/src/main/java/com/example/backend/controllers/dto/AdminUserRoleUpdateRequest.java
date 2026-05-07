package com.example.backend.controllers.dto;

import jakarta.validation.constraints.NotBlank;

public record AdminUserRoleUpdateRequest(@NotBlank String role) {}

