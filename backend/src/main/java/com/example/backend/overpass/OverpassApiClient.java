package com.example.backend.overpass;

import com.example.backend.config.OverpassRestTemplateConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Client for the public Overpass API (OSM data).
 */
@Component
public class OverpassApiClient {

    private static final String INTERPRETER_URL = "https://overpass-api.de/api/interpreter";

    private final RestTemplate overpassRestTemplate;
    private final ObjectMapper objectMapper;

    public OverpassApiClient(
            @Qualifier(OverpassRestTemplateConfig.OVERPASS_REST_TEMPLATE) RestTemplate overpassRestTemplate,
            ObjectMapper objectMapper) {
        this.overpassRestTemplate = overpassRestTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Fetch OSM nodes matching {@code tag} inside the bounding box.
     *
     * @param tag Overpass filter body inside brackets, e.g. {@code amenity=cafe} or {@code shop=supermarket}
     * @param bbox south, west, north, east (WGS84)
     * @return list of elements with id, lat, lon, tags (may be empty)
     */
    public List<OsmElement> fetchByTagAndBBox(String tag, BoundingBox bbox) {
        if (tag == null || tag.isBlank()) {
            throw new IllegalArgumentException("tag must not be null or blank");
        }
        String query = buildOverpassQl(tag.trim(), bbox);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set(HttpHeaders.USER_AGENT, "EcoMapInvest/1.0 (geomarketing import)");

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("data", query);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(form, headers);

        ResponseEntity<String> response;
        try {
            response = overpassRestTemplate.postForEntity(INTERPRETER_URL, request, String.class);
        } catch (RestClientException ex) {
            throw new IllegalStateException("Overpass request failed: " + ex.getMessage(), ex);
        }

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new IllegalStateException("Overpass returned non-2xx or empty body: " + response.getStatusCode());
        }

        return parseElements(response.getBody());
    }

    static String buildOverpassQl(String tag, BoundingBox bbox) {
        return String.format(
                "[out:json][timeout:25];%nnode[%s](%s,%s,%s,%s);%nout body;%n",
                tag,
                Double.toString(bbox.south()),
                Double.toString(bbox.west()),
                Double.toString(bbox.north()),
                Double.toString(bbox.east()));
    }

    private List<OsmElement> parseElements(String jsonBody) {
        try {
            JsonNode root = objectMapper.readTree(jsonBody);
            JsonNode elements = root.path("elements");
            if (!elements.isArray()) {
                return List.of();
            }
            List<OsmElement> out = new ArrayList<>();
            for (JsonNode el : elements) {
                if (!el.path("type").asText("").equals("node")) {
                    continue;
                }
                if (!el.hasNonNull("lat") || !el.hasNonNull("lon") || !el.hasNonNull("id")) {
                    continue;
                }
                long id = el.get("id").asLong();
                double lat = el.get("lat").asDouble();
                double lon = el.get("lon").asDouble();
                Map<String, String> tags = parseTags(el.path("tags"));
                out.add(new OsmElement(id, lat, lon, tags));
            }
            return out;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse Overpass JSON: " + e.getMessage(), e);
        }
    }

    private static Map<String, String> parseTags(JsonNode tagsNode) {
        if (tagsNode == null || !tagsNode.isObject()) {
            return Map.of();
        }
        Map<String, String> tags = new HashMap<>();
        Iterator<String> fieldNames = tagsNode.fieldNames();
        while (fieldNames.hasNext()) {
            String key = fieldNames.next();
            JsonNode v = tagsNode.get(key);
            if (v != null && v.isTextual()) {
                tags.put(key, v.asText());
            }
        }
        return Collections.unmodifiableMap(tags);
    }
}
