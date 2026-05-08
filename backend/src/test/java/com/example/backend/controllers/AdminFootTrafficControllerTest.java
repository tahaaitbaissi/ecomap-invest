package com.example.backend.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.backend.controllers.dto.foottraffic.AdminFootTrafficParamsResponse;
import com.example.backend.controllers.dto.foottraffic.FootTrafficAuditResponse;
import com.example.backend.controllers.dto.foottraffic.FootTrafficRecomputeApiResponse;
import com.example.backend.security.JwtAuthenticationFilter;
import com.example.backend.services.admin.AdminFootTrafficService;
import com.example.backend.testsupport.MethodSecurityExceptionAdvice;
import com.example.backend.testsupport.MethodSecurityTestConfig;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminFootTrafficController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({MethodSecurityTestConfig.class, MethodSecurityExceptionAdvice.class})
class AdminFootTrafficControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminFootTrafficService adminFootTrafficService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @WithMockUser(roles = "USER")
    void listParams_forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/foot-traffic/params")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void listParams_ok() throws Exception {
        when(adminFootTrafficService.listParams()).thenReturn(List.of());
        mockMvc.perform(get("/api/v1/admin/foot-traffic/params")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getParams_notFound() throws Exception {
        when(adminFootTrafficService.getParams("CBD")).thenThrow(new NoSuchElementException("missing"));
        mockMvc.perform(get("/api/v1/admin/foot-traffic/params/CBD")).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void upsertParams_badRequest_on_illegal_args() throws Exception {
        when(adminFootTrafficService.upsert(eq("CBD"), any())).thenThrow(new IllegalArgumentException("bad"));
        mockMvc.perform(
                        put("/api/v1/admin/foot-traffic/params/CBD")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "baseDailyMin": 1,
                                          "baseDailyMax": 2,
                                          "poiDensityCap": 10,
                                          "popDensityCap": 1000,
                                          "incomeWeight": 0,
                                          "hourlyCurveWd": [1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1],
                                          "hourlyCurveSat": [1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1],
                                          "hourlyCurveSun": [1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1],
                                          "seasonalScalers": [1,1,1,1,1,1,1,1,1,1,1,1],
                                          "dayScalerSat": 1,
                                          "dayScalerSun": 1,
                                          "noiseSigma": 0.1
                                        }
                                        """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void upsertParams_ok() throws Exception {
        when(adminFootTrafficService.upsert(eq("CBD"), any()))
                .thenReturn(
                        new AdminFootTrafficParamsResponse(
                                "CBD",
                                1,
                                2,
                                10,
                                1000,
                                0,
                                List.of(1d, 1d, 1d, 1d, 1d, 1d, 1d, 1d, 1d, 1d, 1d, 1d, 1d, 1d, 1d, 1d, 1d, 1d, 1d, 1d, 1d, 1d, 1d, 1d),
                                List.of(1d, 1d, 1d, 1d, 1d, 1d, 1d, 1d, 1d, 1d, 1d, 1d, 1d, 1d, 1d, 1d, 1d, 1d, 1d, 1d, 1d, 1d, 1d, 1d),
                                List.of(1d, 1d, 1d, 1d, 1d, 1d, 1d, 1d, 1d, 1d, 1d, 1d, 1d, 1d, 1d, 1d, 1d, 1d, 1d, 1d, 1d, 1d, 1d, 1d),
                                List.of(1d, 1d, 1d, 1d, 1d, 1d, 1d, 1d, 1d, 1d, 1d, 1d),
                                1,
                                1,
                                0.1,
                                null));

        mockMvc.perform(
                        put("/api/v1/admin/foot-traffic/params/CBD")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "baseDailyMin": 1,
                                          "baseDailyMax": 2,
                                          "poiDensityCap": 10,
                                          "popDensityCap": 1000,
                                          "incomeWeight": 0,
                                          "hourlyCurveWd": [1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1],
                                          "hourlyCurveSat": [1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1],
                                          "hourlyCurveSun": [1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1],
                                          "seasonalScalers": [1,1,1,1,1,1,1,1,1,1,1,1],
                                          "dayScalerSat": 1,
                                          "dayScalerSun": 1,
                                          "noiseSigma": 0.1
                                        }
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.archetype").value("CBD"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void recompute_ok() throws Exception {
        when(adminFootTrafficService.recompute(eq(null), eq("8923")))
                .thenReturn(new FootTrafficRecomputeApiResponse(12, 34, 5L));

        mockMvc.perform(post("/api/v1/admin/foot-traffic/recompute?h3Prefix=8923"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cellsProcessed").value(12));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void audit_ok() throws Exception {
        when(adminFootTrafficService.audit("h"))
                .thenReturn(
                        new FootTrafficAuditResponse(
                                "h",
                                "CBD",
                                0.8,
                                100,
                                20,
                                3,
                                1,
                                0,
                                1000,
                                0,
                                1L,
                                null,
                                Map.of(),
                                List.of(),
                                List.of(),
                                List.of(),
                                1.0));

        mockMvc.perform(get("/api/v1/admin/foot-traffic/audit/h"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.h3Index").value("h"));
    }
}

