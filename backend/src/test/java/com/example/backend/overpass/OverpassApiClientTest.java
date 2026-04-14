package com.example.backend.overpass;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.web.client.RestTemplate;

class OverpassApiClientTest {

    private MockRestServiceServer mockServer;
    private OverpassApiClient client;

    @BeforeEach
    void setUp() {
        RestTemplate rt = new RestTemplate();
        mockServer = MockRestServiceServer.bindTo(rt).build();
        client = new OverpassApiClient(rt, new ObjectMapper());
    }

    @AfterEach
    void tearDown() {
        mockServer.verify();
    }

    @Test
    void buildOverpassQl_matchesSpecShape() {
        BoundingBox bbox = new BoundingBox(33.4, -7.8, 33.7, -7.3);
        String ql = OverpassApiClient.buildOverpassQl("amenity=cafe", bbox);
        assertTrue(ql.contains("[out:json][timeout:25];"));
        assertTrue(ql.contains("node[amenity=cafe](33.4,-7.8,33.7,-7.3);"));
        assertTrue(ql.contains("out body;"));
    }

    @Test
    void fetchByTagAndBBox_parsesElements() {
        BoundingBox bbox = new BoundingBox(33.4, -7.8, 33.7, -7.3);
        String json =
                """
                {"elements":[
                  {"type":"node","id":1,"lat":33.5,"lon":-7.5,"tags":{"name":"Test","amenity":"cafe"}},
                  {"type":"way","id":2},
                  {"type":"node","id":3,"lat":33.6}
                ]}""";

        mockServer
                .expect(MockRestRequestMatchers.requestTo("https://overpass-api.de/api/interpreter"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andExpect(
                        MockRestRequestMatchers.content()
                                .contentTypeCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
                .andRespond(MockRestResponseCreators.withSuccess(json, MediaType.APPLICATION_JSON));

        var result = client.fetchByTagAndBBox("shop=bakery", bbox);
        assertEquals(1, result.size());
        assertEquals(1L, result.getFirst().id());
        assertEquals(33.5, result.getFirst().lat());
        assertEquals(-7.5, result.getFirst().lon());
        assertEquals("Test", result.getFirst().tags().get("name"));
        assertEquals("cafe", result.getFirst().tags().get("amenity"));
    }
}
