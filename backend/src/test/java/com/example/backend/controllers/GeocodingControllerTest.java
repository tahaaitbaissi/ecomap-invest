package com.example.backend.controllers;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.backend.security.JwtAuthenticationFilter;
import com.example.backend.services.GeocodingService;
import com.example.backend.services.GeocodingService.GeocodingResult;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(GeocodingController.class)
@AutoConfigureMockMvc(addFilters = false)
class GeocodingControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private GeocodingService geocodingService;
    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void search_withQuery_ok() throws Exception {
        when(geocodingService.geocode("Casablanca"))
                .thenReturn(Optional.of(new GeocodingResult("Casa", 33.5, -7.6)));
        mockMvc.perform(get("/api/v1/geocode").param("q", "  Casablanca  "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Casa"))
                .andExpect(jsonPath("$.lat").value(33.5))
                .andExpect(jsonPath("$.lng").value(-7.6));
    }

    @Test
    void search_noResult_notFound() throws Exception {
        when(geocodingService.geocode("unknown")).thenReturn(Optional.empty());
        mockMvc.perform(get("/api/v1/geocode").param("q", "unknown")).andExpect(status().isNotFound());
    }

    @Test
    void search_blankQuery_badRequest() throws Exception {
        mockMvc.perform(get("/api/v1/geocode").param("q", "  "))
                .andExpect(status().isBadRequest());
    }
}
