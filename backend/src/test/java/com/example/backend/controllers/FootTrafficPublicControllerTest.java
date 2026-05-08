package com.example.backend.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.backend.foottraffic.entities.FootTrafficCellProfile;
import com.example.backend.foottraffic.services.FootTrafficService;
import com.example.backend.foottraffic.simulation.DayType;
import com.example.backend.security.JwtAuthenticationFilter;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(FootTrafficPublicController.class)
@AutoConfigureMockMvc(addFilters = false)
class FootTrafficPublicControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FootTrafficService footTrafficService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void hourly_bad_day_is_400() throws Exception {
        mockMvc.perform(get("/api/v1/foot-traffic/hourly/h?day=bad"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void hourly_not_found_when_profile_missing() throws Exception {
        when(footTrafficService.getProfile("h")).thenReturn(Optional.empty());
        mockMvc.perform(get("/api/v1/foot-traffic/hourly/h"))
                .andExpect(status().isNotFound());
    }

    @Test
    void hourly_ok() throws Exception {
        var prof = new FootTrafficCellProfile();
        prof.setH3Index("h");
        prof.setArchetype("CBD");
        prof.setBaselineDaily(240);
        prof.setPeakHourly(20);
        when(footTrafficService.getProfile("h")).thenReturn(Optional.of(prof));
        when(footTrafficService.getHourly(eq("h"), eq(DayType.WD), org.mockito.ArgumentMatchers.anyInt()))
                .thenReturn(Optional.of(new double[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24}));

        mockMvc.perform(get("/api/v1/foot-traffic/hourly/h?day=wd&month=5").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.archetype").value("CBD"))
                .andExpect(jsonPath("$.hourly[0]").value(1.0))
                .andExpect(jsonPath("$.hourly[23]").value(24.0));
    }
}

