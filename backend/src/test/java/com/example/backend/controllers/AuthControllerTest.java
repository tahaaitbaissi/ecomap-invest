package com.example.backend.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.backend.controllers.dto.LoginRequest;
import com.example.backend.controllers.dto.RegisterRequest;
import com.example.backend.entities.User;
import com.example.backend.repositories.UserRepository;
import com.example.backend.security.JWTUtil;
import com.example.backend.services.LoginService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private UserRepository userRepository;
    @MockitoBean
    private JWTUtil jwtUtil;
    @MockitoBean
    private AuthenticationManager authenticationManager;
    @MockitoBean
    private PasswordEncoder passwordEncoder;
    @MockitoBean
    private LoginService loginService;

    @Test
    void register_createsUserAndReturnsToken() throws Exception {
        when(userRepository.existsByEmail("a@a.com")).thenReturn(false);
        when(userRepository.existsByUsername("u1")).thenReturn(false);
        when(passwordEncoder.encode("secret")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(
                i -> {
                    User u = i.getArgument(0, User.class);
                    u.setId(1L);
                    return u;
                });
        when(jwtUtil.generateToken("a@a.com")).thenReturn("tok-123");

        var req = new RegisterRequest("u1", "a@a.com", "Name", "secret");
        mockMvc.perform(
                        post("/api/v1/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("tok-123"));
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_duplicateEmail_conflicts() throws Exception {
        when(userRepository.existsByEmail("a@a.com")).thenReturn(true);
        var req = new RegisterRequest("u1", "a@a.com", "Name", "secret");
        mockMvc.perform(
                        post("/api/v1/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    @Test
    void login_validCredentials_returnsToken() throws Exception {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(
                        new UsernamePasswordAuthenticationToken(
                                "a@a.com",
                                "secret",
                                List.of(new SimpleGrantedAuthority("ROLE_USER"))));
        when(jwtUtil.generateToken("a@a.com")).thenReturn("jwt-999");

        var body = new LoginRequest("a@a.com", "secret");
        mockMvc.perform(
                        post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("jwt-999"));
    }

    @Test
    void login_invalidPassword_unauthorized() throws Exception {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new org.springframework.security.authentication.BadCredentialsException("nope"));

        var body = new LoginRequest("a@a.com", "wrong");
        mockMvc.perform(
                        post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnauthorized());
    }
}
