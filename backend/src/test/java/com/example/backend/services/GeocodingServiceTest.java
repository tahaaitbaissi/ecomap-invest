package com.example.backend.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class GeocodingServiceTest {

    private MockWebServer server;
    private GeocodingService service;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        RestClient c = RestClient.builder()
                .baseUrl("http://127.0.0.1:" + server.getPort())
                .defaultHeader("User-Agent", "test")
                .build();
        service = new GeocodingService(c);
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    void search_parsesResults() {
        String body =
                """
                [
                  {
                    "display_name": "Casablanca, MA",
                    "lat": "33.5",
                    "lon": "-7.6"
                  }
                ]
                """;
        server.enqueue(new MockResponse()
                .addHeader("Content-Type", "application/json")
                .setBody(body));

        var r = service.search("cas");
        assertEquals(1, r.size());
        assertEquals("Casablanca, MA", r.get(0).displayName());
        assertEquals(33.5, r.get(0).lat());
        assertEquals(-7.6, r.get(0).lng());
    }

    @Test
    void search_nullBody_returnsEmpty() {
        server.enqueue(
                new MockResponse().addHeader("Content-Type", "application/json").setBody("null"));
        var r = service.search("x");
        assertTrue(r.isEmpty());
    }
}
