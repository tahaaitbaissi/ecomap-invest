package com.example.backend.controllers.dto;

import java.util.List;

public record ProfileTagOptionResponse(
        String tag,
        String label,
        String group,
        String description,
        List<String> aliases
) {}
