package com.example.backend.controllers.dto;

import java.util.UUID;

public record AdminUserListItemResponse(
        UUID id,
        String email,
        String companyName,
        String role,
        String createdAt) {}

