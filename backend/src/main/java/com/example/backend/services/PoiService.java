package com.example.backend.services;

import com.example.backend.controllers.dto.PoiMapResponse;
import com.example.backend.entities.Poi;
import com.example.backend.repositories.PoiRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PoiService {

    private final PoiRepository poiRepository;

    @Transactional(readOnly = true)
    public List<PoiMapResponse> getPoisInBoundingBox(double swLng, double swLat, double neLng, double neLat) {
        return poiRepository.findAllInBoundingBox(swLng, swLat, neLng, neLat)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private PoiMapResponse toResponse(Poi poi) {
        return new PoiMapResponse(
                poi.getId(),
                poi.getName(),
                poi.getAddress(),
                poi.getTypeTag(),
                poi.getLocation().getY(),
                poi.getLocation().getX()
        );
    }
}
