package com.example.backend.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.backend.controllers.dto.AdminDemographicsResponse;
import com.example.backend.security.JwtAuthenticationFilter;
import com.example.backend.services.admin.AdminDemographicsService;
import com.example.backend.testsupport.MethodSecurityExceptionAdvice;
import com.example.backend.testsupport.MethodSecurityTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminDemographicsController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({MethodSecurityTestConfig.class, MethodSecurityExceptionAdvice.class})
class AdminDemographicsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminDemographicsService adminDemographicsService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @WithMockUser(roles = "USER")
    void get_forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/demographics/abc")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void upsert_ok() throws Exception {
        when(adminDemographicsService.upsert(eq("h3"), any()))
                .thenReturn(new AdminDemographicsResponse("h3", 1.2, 3.4, "2026-01-01T00:00:00Z"));

        mockMvc.perform(put("/api/v1/admin/demographics/h3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"populationDensity":1.2,"avgIncome":3.4}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.h3Index").value("h3"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void get_ok() throws Exception {
        when(adminDemographicsService.get("h3")).thenReturn(new AdminDemographicsResponse("h3", 1.0, 2.0, null));
        mockMvc.perform(get("/api/v1/admin/demographics/h3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.h3Index").value("h3"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void delete_noContent() throws Exception {
        doNothing().when(adminDemographicsService).delete("h3");
        mockMvc.perform(delete("/api/v1/admin/demographics/h3")).andExpect(status().isNoContent());
    }
}

