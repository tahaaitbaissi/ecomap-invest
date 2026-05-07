package com.example.backend.controllers;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.backend.audit.AuditLog;
import com.example.backend.audit.AuditLogRepository;
import com.example.backend.security.JwtAuthenticationFilter;
import com.example.backend.testsupport.MethodSecurityExceptionAdvice;
import com.example.backend.testsupport.MethodSecurityTestConfig;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminAuditController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({MethodSecurityTestConfig.class, MethodSecurityExceptionAdvice.class})
class AdminAuditControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuditLogRepository auditLogRepository;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @WithMockUser(roles = "USER")
    void list_forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/audit")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void list_ok() throws Exception {
        AuditLog l = new AuditLog();
        l.setId(1L);
        l.setAuditId(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));
        l.setOccurredAt(Instant.parse("2026-01-01T00:00:00Z"));
        l.setUserEmail("a@b.com");
        l.setAction("X");
        when(auditLogRepository.findAll(
                        org.mockito.ArgumentMatchers.<Specification<AuditLog>>any(),
                        eq(PageRequest.of(0, 50, Sort.by(Sort.Direction.DESC, "occurredAt")))))
                .thenReturn(new PageImpl<>(List.of(l)));

        mockMvc.perform(get("/api/v1/admin/audit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(1L))
                .andExpect(jsonPath("$.items[0].userEmail").value("a@b.com"));
    }

    private static <T> T eq(T v) {
        return org.mockito.ArgumentMatchers.eq(v);
    }
}

