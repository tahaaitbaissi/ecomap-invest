package com.example.backend.controllers;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.backend.controllers.dto.HexagonMapResponse;
import com.example.backend.security.JwtAuthenticationFilter;
import com.example.backend.scoring.HexagonRawScoringSupport;
import com.example.backend.services.HexagonScoringService;
import com.example.backend.services.profile.DynamicProfileService;
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
    private DynamicProfileService dynamicProfileService;
    @MockitoBean
    private HexagonRawScoringSupport hexagonRawScoringSupport;
    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @WithMockUser(roles = "INVESTOR")
    void getHexagons_grayMode_ok() throws Exception {
        when(hexagonScoringService.getHexagonsInBbox(any(String.class), isNull(), isNull()))
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

        verify(hexagonScoringService).getHexagonsInBbox("-7.6,33.5,-7.5,33.6", null, null);
    }

    @Test
    @WithMockUser(roles = "INVESTOR")
    void getHexagons_badBbox_400() throws Exception {
        when(hexagonScoringService.getHexagonsInBbox("bad", null, null))
                .thenThrow(new IllegalArgumentException("bbox is required"));

        mockMvc.perform(get("/api/v1/hexagons").param("bbox", "bad"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$").isString());
    }

    @Test
    @WithMockUser(roles = "INVESTOR")
    void getHexagons_profileNotFound_404() throws Exception {
        UUID id = UUID.fromString("00000000-0000-0000-0000-000000000001");
        when(hexagonScoringService.getHexagonsInBbox(any(String.class), eq(id), isNull()))
                .thenThrow(new NoSuchElementException("Unknown profile: " + id));

        mockMvc.perform(
                        get("/api/v1/hexagons")
                                .param("bbox", "-7.6,33.5,-7.5,33.6")
                                .param("profileId", id.toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "INVESTOR")
    void getHexagons_h3ResolutionOutOfRange_badRequest() throws Exception {
        when(hexagonScoringService.getHexagonsInBbox(any(String.class), isNull(), eq(6)))
                .thenThrow(new IllegalArgumentException("h3Resolution must be between 7 and 9 inclusive"));

        mockMvc.perform(get("/api/v1/hexagons")
                        .param("bbox", "-7.6,33.5,-7.5,33.6")
                        .param("h3Resolution", "6"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "INVESTOR")
    void getHexagons_h3Resolution7_ok() throws Exception {
        when(hexagonScoringService.getHexagonsInBbox(any(String.class), isNull(), eq(7)))
                .thenReturn(
                        List.of(
                                new HexagonMapResponse(
                                        "8729a10ffffff",
                                        40.0,
                                        List.of(
                                                new HexagonMapResponse.LatLng(33.0, -7.5),
                                                new HexagonMapResponse.LatLng(33.01, -7.5)))));

        mockMvc.perform(get("/api/v1/hexagons")
                        .param("bbox", "-7.6,33.5,-7.5,33.6")
                        .param("h3Resolution", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].h3Index").value("8729a10ffffff"));

        verify(hexagonScoringService).getHexagonsInBbox("-7.6,33.5,-7.5,33.6", null, 7);
    }

    @Test
    @WithMockUser(roles = "INVESTOR")
    void byH3Index_valid_ok() throws Exception {
        when(hexagonRawScoringSupport.boundaryPoints("891ea6c0d47ffff"))
                .thenReturn(
                        List.of(
                                new HexagonRawScoringSupport.LatLngRingPoint(33.0, -7.5),
                                new HexagonRawScoringSupport.LatLngRingPoint(33.01, -7.5)));

        mockMvc.perform(get("/api/v1/hexagons/h3/891ea6c0d47ffff"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.h3Index").value("891ea6c0d47ffff"))
                .andExpect(jsonPath("$.score").value(nullValue()))
                .andExpect(jsonPath("$.boundary.length()").value(2));
    }

    @Test
    @WithMockUser(roles = "INVESTOR")
    void byH3Index_invalid_400() throws Exception {
        when(hexagonRawScoringSupport.boundaryPoints("not-an-h3"))
                .thenThrow(new IllegalArgumentException("invalid H3 index"));

        mockMvc.perform(get("/api/v1/hexagons/h3/not-an-h3"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$").isString());
    }
}
