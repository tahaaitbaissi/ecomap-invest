package com.example.backend.controllers.dto;

import java.util.UUID;

public record PoiMapResponse(
        UUID id,
        String name,
        String address,
        String typeTag,
        double latitude,
<<<<<<< HEAD
        double longitude
=======
        double longitude,
        Double saturationScore
>>>>>>> 246537c (feat: add axios instance with request/response interceptors)
) {
}
