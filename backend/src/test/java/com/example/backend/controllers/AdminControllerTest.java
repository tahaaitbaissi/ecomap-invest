package com.example.backend.controllers;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.backend.security.JwtAuthenticationFilter;
import com.example.backend.services.CsvIngestionService;
import com.example.backend.services.EnterpriseXlsxIngestionService;
import com.example.backend.testsupport.MethodSecurityExceptionAdvice;
import com.example.backend.testsupport.MethodSecurityTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({MethodSecurityTestConfig.class, MethodSecurityExceptionAdvice.class})
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private CsvIngestionService csvIngestionService;
    @MockitoBean
    private EnterpriseXlsxIngestionService enterpriseXlsxIngestionService;
    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @WithMockUser(roles = "ADMIN")
    void ingest_ok() throws Exception {
        when(csvIngestionService.ingestFromCsv("data/mock_poi.csv")).thenReturn(3);
        mockMvc.perform(post("/api/v1/admin/ingest-csv"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rowsInserted").value(3));
    }

    @Test
    @WithMockUser(roles = "USER")
    void ingest_forbiddenForUser() throws Exception {
        mockMvc.perform(post("/api/v1/admin/ingest-csv")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void ingestEnterprises_ok() throws Exception {
        when(enterpriseXlsxIngestionService.ingestFromClasspath("data/enterprises.xlsx")).thenReturn(42);
        mockMvc.perform(post("/api/v1/admin/ingest-enterprises"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rowsInserted").value(42));
    }
}
