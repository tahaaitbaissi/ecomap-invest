package com.example.backend.controllers;

import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.backend.security.JwtAuthenticationFilter;
import com.example.backend.services.rmi.RmiScoringClient;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(RmiScoringController.class)
@AutoConfigureMockMvc(addFilters = false)
class RmiScoringControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private RmiScoringClient rmiScoringClient;
    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void score_ok() throws Exception {
        when(rmiScoringClient.computeSaturationScore(1, 2, 0.3))
                .thenReturn(CompletableFuture.completedFuture(42.5));
        mockMvc.perform(get("/api/v1/rmi/score")
                        .param("drivers", "1")
                        .param("competitors", "2")
                        .param("density", "0.3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(42.5))
                .andExpect(jsonPath("$.source").value("rmi"));
    }

    @Test
    void score_failure_serviceUnavailable() throws Exception {
        reset(rmiScoringClient);
        when(rmiScoringClient.computeSaturationScore(0, 0, 0.5))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("down")));
        mockMvc.perform(get("/api/v1/rmi/score"))
                .andExpect(status().isServiceUnavailable());
        verify(rmiScoringClient).resetStub();
    }

    @Test
    void ping_ok() throws Exception {
        when(rmiScoringClient.ping()).thenReturn(CompletableFuture.completedFuture("pong"));
        mockMvc.perform(get("/api/v1/rmi/ping"))
                .andExpect(status().isOk())
                .andExpect(content().string("pong"));
    }
}
