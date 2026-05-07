package com.example.backend.controllers;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.backend.entities.User;
import com.example.backend.repositories.UserRepository;
import com.example.backend.security.JwtAuthenticationFilter;
import com.example.backend.testsupport.MethodSecurityExceptionAdvice;
import com.example.backend.testsupport.MethodSecurityTestConfig;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminUserController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({MethodSecurityTestConfig.class, MethodSecurityExceptionAdvice.class})
class AdminUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @WithMockUser(roles = "USER")
    void list_forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void list_ok() throws Exception {
        User u = new User();
        u.setId(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));
        u.setEmail("a@b.com");
        u.setRole("ROLE_INVESTOR");
        u.setCreatedAt(Timestamp.from(java.time.Instant.parse("2026-01-01T00:00:00Z")));
        when(userRepository.findAll(PageRequest.of(0, 25, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"))))
                .thenReturn(new PageImpl<>(List.of(u)));

        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].email").value("a@b.com"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void setRole_ok() throws Exception {
        UUID id = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
        User u = new User();
        u.setId(id);
        u.setEmail("x@y.com");
        u.setRole("ROLE_INVESTOR");
        when(userRepository.findById(id)).thenReturn(java.util.Optional.of(u));
        when(userRepository.save(org.mockito.ArgumentMatchers.any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(patch("/api/v1/admin/users/" + id + "/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"role":"ROLE_ADMIN"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ROLE_ADMIN"));
    }
}

