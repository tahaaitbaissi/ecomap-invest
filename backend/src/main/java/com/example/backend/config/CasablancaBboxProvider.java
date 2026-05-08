package com.example.backend.config;

import com.example.backend.services.GeoViewbox;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CasablancaBboxProvider {

    private final CasablancaStudyAreaProperties studyArea;

    /** Study-area envelope as a Nominatim {@code viewbox} (SW → NE). */
    public GeoViewbox casablancaViewbox() {
        return new GeoViewbox(studyArea.swLng(), studyArea.swLat(), studyArea.neLng(), studyArea.neLat());
    }
}
