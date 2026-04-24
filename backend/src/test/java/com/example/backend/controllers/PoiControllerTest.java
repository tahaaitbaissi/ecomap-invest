package com.example.backend.controllers;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.backend.security.JwtAuthenticationFilter;
import com.example.backend.services.PoiService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PoiController.class)
@AutoConfigureMockMvc(addFilters = false)
class PoiControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private PoiService poiService;
    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void getPois_validBbox_ok() throws Exception {
        var id = UUID.randomUUID();
        when(poiService.getPoisInBoundingBox(-7.7, 33.5, -7.5, 33.6))
                .thenReturn(
                        List.of(
                                new com.example.backend.controllers.dto.PoiMapResponse(
                                        id, "N", "A", "shop=x", 33.5, -7.6, 12.0)));
        mockMvc.perform(get("/api/v1/poi").param("bbox", "-7.7,33.5,-7.5,33.6"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("N"));
    }

    @Test
    void getPois_invalidBboxCount_badRequest() throws Exception {
        mockMvc.perform(get("/api/v1/poi").param("bbox", "1,2,3"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getPois_nonNumericBbox_badRequest() throws Exception {
        mockMvc.perform(get("/api/v1/poi").param("bbox", "a,b,c,d"))
                .andExpect(status().isBadRequest());
    }
}
