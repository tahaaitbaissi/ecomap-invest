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
    private NominatimSearchClient client;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        RestClient c = RestClient.builder()
                .baseUrl("http://127.0.0.1:" + server.getPort())
                .defaultHeader("User-Agent", "test")
                .build();
        client = new NominatimSearchClient(c);
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    void fetchFirst_parsesFirstResult() {
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

        var r = client.fetchFirst("cas");
        assertTrue(r.isPresent());
        assertEquals("Casablanca, MA", r.get().displayName());
        assertEquals(33.5, r.get().lat());
        assertEquals(-7.6, r.get().lng());
    }

    @Test
    void fetchFirst_nullBody_returnsEmpty() {
        server.enqueue(
                new MockResponse().addHeader("Content-Type", "application/json").setBody("null"));
        var r = client.fetchFirst("x");
        assertTrue(r.isEmpty());
    }
}
