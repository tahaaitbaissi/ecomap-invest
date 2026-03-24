package com.example.backend.controllers.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(min = 3, max = 50) String username,
        @Email String email,
        @Size(min = 2, max = 100) String name
) {
}
