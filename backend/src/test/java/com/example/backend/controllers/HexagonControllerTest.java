package com.example.backend.controllers;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.backend.controllers.dto.HexagonMapResponse;
import com.example.backend.security.JwtAuthenticationFilter;
import com.example.backend.services.HexagonScoringService;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(HexagonController.class)
@AutoConfigureMockMvc(addFilters = false)
class HexagonControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HexagonScoringService hexagonScoringService;
    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void getHexagons_grayMode_ok() throws Exception {
        when(hexagonScoringService.getHexagonsInBbox(any(String.class), isNull()))
                .thenReturn(
                        List.of(
                                new HexagonMapResponse(
                                        "891ea6c0d47ffff",
                                        null,
                                        List.of(
                                                new HexagonMapResponse.LatLng(33.0, -7.5),
                                                new HexagonMapResponse.LatLng(33.01, -7.5)))));

        mockMvc.perform(get("/api/v1/hexagons").param("bbox", "-7.6,33.5,-7.5,33.6"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("max-age=3600")))
                .andExpect(jsonPath("$[0].h3Index").value("891ea6c0d47ffff"))
                .andExpect(jsonPath("$[0].score").value(nullValue()));

        verify(hexagonScoringService).getHexagonsInBbox("-7.6,33.5,-7.5,33.6", null);
    }

    @Test
    void getHexagons_badBbox_400() throws Exception {
        when(hexagonScoringService.getHexagonsInBbox("bad", null))
                .thenThrow(new IllegalArgumentException("bbox is required"));

        mockMvc.perform(get("/api/v1/hexagons").param("bbox", "bad"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$").isString());
    }

    @Test
    @WithMockUser
    void getHexagons_profileNotFound_404() throws Exception {
        UUID id = UUID.fromString("00000000-0000-0000-0000-000000000001");
        when(hexagonScoringService.getHexagonsInBbox(any(String.class), eq(id)))
                .thenThrow(new NoSuchElementException("Unknown profile: " + id));

        mockMvc.perform(
                        get("/api/v1/hexagons")
                                .param("bbox", "-7.6,33.5,-7.5,33.6")
                                .param("profileId", id.toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    void getHexagons_profileWithoutJwt_unauthorized() throws Exception {
        UUID id = UUID.fromString("00000000-0000-0000-0000-000000000002");
        mockMvc.perform(
                        get("/api/v1/hexagons")
                                .param("bbox", "-7.6,33.5,-7.5,33.6")
                                .param("profileId", id.toString()))
                .andExpect(status().isUnauthorized());
        verify(hexagonScoringService, never()).getHexagonsInBbox(any(), any());
    }
}
