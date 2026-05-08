package com.example.backend.services;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.example.backend.controllers.dto.GeocodingSuggestionResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class NominatimSearchClientTest {

    private MockRestServiceServer mockServer;
    private NominatimSearchClient client;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("https://nominatim.openstreetmap.org/search")
                .defaultHeader("User-Agent", "EcoMapInvest/1.0-test");
        mockServer = MockRestServiceServer.bindTo(builder).build();
        client = new NominatimSearchClient(builder.build());
    }

    @AfterEach
    void tearDown() {
        mockServer.verify();
    }

    @Test
    void fetchMany_propagatesViewboxLimitAndBounded() throws Exception {
        GeoViewbox vb = new GeoViewbox(-7.8, 33.4, -7.4, 33.7);

        List<Map<String, Object>> payload = List.of(
                Map.of("display_name", "X", "lat", "33.5", "lon", "-7.6", "boundingbox", List.of("33.4", "33.6", "-7.7", "-7.5")));
        String json = objectMapper.writeValueAsString(payload);

        mockServer
                .expect(requestTo(allOf(
                        startsWith("https://nominatim.openstreetmap.org/search?"),
                        containsString("q=Maarif"),
                        containsString("limit=5"),
                        containsString("bounded=1"),
                        containsString("countrycodes=MA"),
                        containsString("addressdetails=0"),
                        containsString("dedupe=1"),
                        containsString("viewbox="),
                        containsString("-7.8"),
                        containsString("33.4"),
                        containsString("-7.4"),
                        containsString("33.7"))))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        List<GeocodingSuggestionResponse> out = client.fetchMany("Maarif", 5, vb);
        assertEquals(1, out.size());
        assertEquals("X", out.get(0).displayName());
        assertEquals(33.4, out.get(0).southLat());
        assertEquals(33.6, out.get(0).northLat());
        assertEquals(-7.7, out.get(0).westLng());
        assertEquals(-7.5, out.get(0).eastLng());
    }

    @Test
    void fetchMany_capsLimitAt10() throws Exception {
        mockServer
                .expect(requestTo(allOf(
                        startsWith("https://nominatim.openstreetmap.org/search?"),
                        containsString("q=q"),
                        containsString("limit=10"))))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertTrue(client.fetchMany("q", 99, new GeoViewbox(-1, -1, 1, 1)).isEmpty());
    }
}
