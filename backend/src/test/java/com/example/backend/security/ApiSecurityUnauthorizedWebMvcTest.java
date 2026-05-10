package com.example.backend.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.backend.config.SecurityConfig;
import com.example.backend.controllers.PoiController;
import com.example.backend.services.LoginService;
import com.example.backend.services.PoiService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Smoke: verifies the JWT + {@link SecurityConfig} chain returns 401 for protected APIs when no
 * {@code Authorization} header is sent. No Docker — complements {@code @Tag("integration")} tests.
 */
@WebMvcTest(controllers = PoiController.class)
@AutoConfigureMockMvc
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
@TestPropertySource(
        properties = {
            "jwt.secret=TestJwtSecretKeyForUnitAndIntegrationTestsMustBe256BitLongString!!",
            "jwt.expiration=86400000",
            "app.security.allow-open-registration=true"
        })
class ApiSecurityUnauthorizedWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PoiService poiService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private LoginService loginService;

    @Test
    void poi_withoutBearer_isUnauthorized() throws Exception {
        mockMvc.perform(
                        get("/api/v1/poi")
                                .param("minX", "-7.7")
                                .param("minY", "33.5")
                                .param("maxX", "-7.5")
                                .param("maxY", "33.6"))
                .andExpect(status().isUnauthorized());
    }
}
