package com.example.backend.services;

import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Slf4j
@Service
public class GeocodingService {

    private final RestClient restClient;

        public GeocodingService(@Qualifier("geocodingRestClient") RestClient restClient) {
                this.restClient = restClient;
    }

    @SuppressWarnings("unchecked")
    public List<GeocodingResult> search(String query) {
        List<Map<String, Object>> raw = restClient.get()
                .uri(uri -> uri
                        .queryParam("q", query)
                        .queryParam("format", "json")
                        .queryParam("limit", 5)
                        .build())
                .retrieve()
                .body(List.class);

        if (raw == null) return List.of();

        return raw.stream()
                .map(m -> new GeocodingResult(
                        (String) m.get("display_name"),
                        Double.parseDouble(String.valueOf(m.get("lat"))),
                        Double.parseDouble(String.valueOf(m.get("lon")))
                ))
                .toList();
    }

    public record GeocodingResult(String displayName, double lat, double lng) {}
}
