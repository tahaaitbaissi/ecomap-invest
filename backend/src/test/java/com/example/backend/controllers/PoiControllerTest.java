package com.example.backend.controllers;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
    void getPois_validViewport_ok() throws Exception {
        var id = UUID.randomUUID();
        when(poiService.getPoisInBoundingBox(-7.7, 33.5, -7.5, 33.6))
                .thenReturn(
                        List.of(
                                new com.example.backend.controllers.dto.PoiMapResponse(
                                        id, "N", "A", "shop=x", 33.5, -7.6, 12.0)));
        mockMvc.perform(get("/api/v1/poi")
                        .param("minX", "-7.7")
                        .param("minY", "33.5")
                        .param("maxX", "-7.5")
                        .param("maxY", "33.6"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("max-age=300")))
                .andExpect(jsonPath("$[0].name").value("N"));
    }

    @Test
    void getPois_viewportExceedsMaxSpan_badRequest() throws Exception {
        mockMvc.perform(get("/api/v1/poi")
                        .param("minX", "-7.8")
                        .param("minY", "33.5")
                        .param("maxX", "-6")
                        .param("maxY", "33.6"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getPois_missingParam_badRequest() throws Exception {
        mockMvc.perform(get("/api/v1/poi")
                        .param("minX", "1")
                        .param("minY", "2")
                        .param("maxX", "3"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getPois_nonNumericParam_badRequest() throws Exception {
        mockMvc.perform(get("/api/v1/poi")
                        .param("minX", "a")
                        .param("minY", "b")
                        .param("maxX", "c")
                        .param("maxY", "d"))
                .andExpect(status().isBadRequest());
    }
}
