package com.example.backend.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.backend.controllers.dto.AdminPoiResponse;
import com.example.backend.security.JwtAuthenticationFilter;
import com.example.backend.services.admin.AdminPoiService;
import com.example.backend.testsupport.MethodSecurityExceptionAdvice;
import com.example.backend.testsupport.MethodSecurityTestConfig;
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
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminPoiController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({MethodSecurityTestConfig.class, MethodSecurityExceptionAdvice.class})
class AdminPoiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminPoiService adminPoiService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @WithMockUser(roles = "USER")
    void search_forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/poi/search")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void search_ok() throws Exception {
        UUID id = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        var pr = PageRequest.of(0, 50, Sort.by(Sort.Direction.DESC, "importedAt"));
        when(adminPoiService.searchPage(any(), any(), any(), any(), eq("category=school"), eq("mar"), eq(pr)))
                .thenReturn(new PageImpl<>(
                        List.of(new AdminPoiResponse(id, "node/1", "A", null, "category=school", 1.0, 2.0, null, null, null)),
                        pr,
                        1));

        mockMvc.perform(get("/api/v1/admin/poi/search")
                        .param("typeTag", "category=school")
                        .param("nameLike", "mar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.items[0].id").value(id.toString()));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_ok() throws Exception {
        UUID id = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
        when(adminPoiService.create(any()))
                .thenReturn(new AdminPoiResponse(id, "manual:x", "X", null, "category=office", 3.0, 4.0, null, null, null));

        mockMvc.perform(post("/api/v1/admin/poi")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"typeTag":"category=office","lat":3.0,"lng":4.0}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.osmId").value("manual:x"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void update_ok() throws Exception {
        UUID id = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
        when(adminPoiService.update(eq(id), any()))
                .thenReturn(new AdminPoiResponse(id, "node/9", "Y", null, "category=office", 3.0, 4.0, null, null, null));

        mockMvc.perform(patch("/api/v1/admin/poi/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"typeTag":"category=office","lat":3.0,"lng":4.0}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void delete_noContent() throws Exception {
        UUID id = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
        mockMvc.perform(delete("/api/v1/admin/poi/" + id)).andExpect(status().isNoContent());
    }
}

