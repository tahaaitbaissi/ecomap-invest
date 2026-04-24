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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Client for the public Overpass API (OSM data). Uses several public mirrors, retries, and
 * long HTTP read timeouts; the main instance (overpass-api.de) often returns 504 when busy.
 */
@Component
public class OverpassApiClient {

    private static final Logger log = LoggerFactory.getLogger(OverpassApiClient.class);

    private final List<String> interpreterUrls;
    private final int fullRetryPasses;
    private final RestTemplate overpassRestTemplate;
    private final ObjectMapper objectMapper;

    public OverpassApiClient(
            @Qualifier(OverpassRestTemplateConfig.OVERPASS_REST_TEMPLATE) RestTemplate overpassRestTemplate,
            ObjectMapper objectMapper,
            @Value(
                    "${app.overpass.interpreter-urls:https://overpass-api.de/api/interpreter,https://overpass.kumi.systems/api/interpreter,https://overpass.openstreetmap.fr/api/interpreter}")
                    String interpreterUrls,
            @Value("${app.overpass.full-retry-passes:3}") int fullRetryPasses) {
        this.overpassRestTemplate = overpassRestTemplate;
        this.objectMapper = objectMapper;
        this.interpreterUrls = parseUrls(interpreterUrls);
        this.fullRetryPasses = Math.max(1, fullRetryPasses);
    }

    private static List<String> parseUrls(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of("https://overpass-api.de/api/interpreter");
        }
        List<String> out = new ArrayList<>();
        for (String s : raw.split(",")) {
            String t = s.trim();
            if (!t.isEmpty()) {
                out.add(t);
            }
        }
        return out.isEmpty() ? List.of("https://overpass-api.de/api/interpreter") : List.copyOf(out);
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
        headers.set(HttpHeaders.USER_AGENT, "EcoMapInvest/1.0 (geomarketing import; contact: project)");

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("data", query);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(form, headers);

        Exception last = null;
        for (int pass = 0; pass < fullRetryPasses; pass++) {
            for (String url : interpreterUrls) {
                try {
                    ResponseEntity<String> response = overpassRestTemplate.postForEntity(url, request, String.class);
                    if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                        if (response.getBody().contains("runtime error")
                                && response.getBody().contains("timeout")) {
                            log.warn("Overpass busy/timeout on {} (body), trying next", url);
                            continue;
                        }
                        return parseElements(response.getBody());
                    }
                } catch (HttpStatusCodeException ex) {
                    if (isRetryableStatus(ex.getStatusCode())) {
                        log.warn("Overpass {} on {}: {}", ex.getStatusCode().value(), url, ex.getMessage());
                        last = ex;
                        continue;
                    }
                    last = ex;
                } catch (ResourceAccessException ex) {
                    log.warn("Overpass I/O on {}: {}", url, ex.getMessage());
                    last = ex;
                } catch (RestClientException ex) {
                    last = ex;
                }
            }
            if (pass < fullRetryPasses - 1) {
                long waitMs = 1000L * (pass + 1) * 2L;
                log.info("Overpass: full mirror pass {} failed, waiting {} ms before next pass", pass + 1, waitMs);
                try {
                    Thread.sleep(waitMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Interrupted while waiting to retry Overpass", e);
                }
            }
        }
        if (last != null) {
            throw new IllegalStateException("Overpass request failed on all mirrors after retries: " + last.getMessage(), last);
        }
        throw new IllegalStateException("Overpass request failed: no response");
    }

    private static boolean isRetryableStatus(HttpStatusCode code) {
        int n = code.value();
        return n == 502 || n == 503 || n == 504 || n == 429;
    }

    static String buildOverpassQl(String tag, BoundingBox bbox) {
        return String.format(
                "[out:json][timeout:60];%nnode[%s](%s,%s,%s,%s);%nout body;%n",
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
