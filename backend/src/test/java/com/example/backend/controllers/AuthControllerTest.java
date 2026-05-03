package com.example.backend.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.backend.controllers.dto.LoginRequest;
import com.example.backend.controllers.dto.RegisterRequest;
import com.example.backend.repositories.UserRepository;
import com.example.backend.security.JwtService;
import com.example.backend.services.AuthService;
import com.example.backend.services.LoginService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.core.userdetails.UserDetails;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private UserRepository userRepository;
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private AuthService authService;
    @MockitoBean
    private AuthenticationManager authenticationManager;
    @MockitoBean
    private LoginService loginService;

    @Test
    void register_createsUserAndReturnsToken() throws Exception {
        org.mockito.Mockito.when(authService.register(any(RegisterRequest.class)))
                .thenReturn(new com.example.backend.controllers.dto.AuthResponse(
                        "tok-123", 86400L, "a@a.com", "ROLE_INVESTOR"
                ));

        var req = new RegisterRequest("a@a.com", "Secret123", "MyCo");
        mockMvc.perform(
                        post("/api/v1/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("tok-123"))
                .andExpect(jsonPath("$.expiresIn").value(86400))
                .andExpect(jsonPath("$.userEmail").value("a@a.com"))
                .andExpect(jsonPath("$.role").value("ROLE_INVESTOR"));
    }

    @Test
    void register_duplicateEmail_conflicts() throws Exception {
        org.mockito.Mockito.when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new com.example.backend.exceptions.EmailAlreadyExistsException("a@a.com"));

        var req = new RegisterRequest("a@a.com", "Secret123", "MyCo");
        mockMvc.perform(
                        post("/api/v1/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    @Test
    void login_validCredentials_returnsToken() throws Exception {
        org.mockito.Mockito.when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(new UsernamePasswordAuthenticationToken("a@a.com", "secret"));

        var u = new com.example.backend.entities.User();
        u.setEmail("a@a.com");
        u.setRole("ROLE_INVESTOR");
        org.mockito.Mockito.when(userRepository.findByEmail("a@a.com")).thenReturn(java.util.Optional.of(u));
        UserDetails ud =
                org.springframework.security.core.userdetails.User.builder()
                        .username("a@a.com")
                        .password("x")
                        .authorities("ROLE_INVESTOR")
                        .build();
        org.mockito.Mockito.when(loginService.loadUserByUsername("a@a.com")).thenReturn(ud);
        org.mockito.Mockito.when(jwtService.generateToken(org.mockito.ArgumentMatchers.any(UserDetails.class)))
                .thenReturn("jwt-999");

        var body = new LoginRequest("a@a.com", "secret");
        mockMvc.perform(
                        post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-999"))
                .andExpect(jsonPath("$.userEmail").value("a@a.com"))
                .andExpect(jsonPath("$.role").value("ROLE_INVESTOR"));
    }

    @Test
    void login_invalidPassword_unauthorized() throws Exception {
        org.mockito.Mockito.when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new org.springframework.security.authentication.BadCredentialsException("nope"));

        var body = new LoginRequest("a@a.com", "wrong");
        mockMvc.perform(
                        post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnauthorized());
    }
}
