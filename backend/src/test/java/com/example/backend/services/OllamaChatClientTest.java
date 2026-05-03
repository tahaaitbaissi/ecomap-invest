package com.example.backend.services;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.backend.config.OllamaConfiguration;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

class OllamaChatClientTest {

    private MockWebServer server;
    private OllamaChatClient client;

    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
        RestClient rc = new OllamaConfiguration()
                .ollamaRestClient("http://127.0.0.1:" + server.getPort());
        client = new OllamaChatClient(rc);
        ReflectionTestUtils.setField(client, "modelName", "llama3");
    }

    @AfterEach
    void tearDown() throws Exception {
        server.shutdown();
    }

    @Test
    void generate_parsesWhenOllamaReturnsJson() {
        server.enqueue(
                new MockResponse()
                        .addHeader("Content-Type", "application/json")
                        .setBody("{\"response\":\"ok\"}"));

        String out = client.generate("hello");
        assertThat(out).contains("ok");
    }
}
