package com.example.backend.controllers.dto;

import com.example.backend.entities.Role;

public record ProfileResponse(
        Long id,
        String username,
        String email,
        String name,
        Role role
) {
}
