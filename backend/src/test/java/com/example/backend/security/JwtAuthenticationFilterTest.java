package com.example.backend.security;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.backend.services.LoginService;
import jakarta.servlet.FilterChain;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.AfterEach;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JWTUtil jwtUtil;

    @Mock
    private LoginService loginService;

    @Mock
    private FilterChain filterChain;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        filter = new JwtAuthenticationFilter(jwtUtil, loginService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void noAuthorizationHeader_proceedsWithoutAuth() throws Exception {
        var req = new MockHttpServletRequest();
        var res = new MockHttpServletResponse();
        filter.doFilterInternal(req, res, filterChain);
        verify(filterChain).doFilter(req, res);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void invalidToken_proceedsWithoutSettingAuth() throws Exception {
        var req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer bad");
        var res = new MockHttpServletResponse();
        when(jwtUtil.getUsernameFromToken("bad")).thenThrow(new RuntimeException("parse"));
        filter.doFilterInternal(req, res, filterChain);
        verify(filterChain).doFilter(req, res);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void validToken_setsAuthentication() throws Exception {
        var req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer good.tok");
        var res = new MockHttpServletResponse();
        when(jwtUtil.getUsernameFromToken("good.tok")).thenReturn("a@a.com");
        UserDetails ud =
                User.builder()
                        .username("a@a.com")
                        .password("x")
                        .authorities(Collections.emptyList())
                        .build();
        when(loginService.loadUserByUsername("a@a.com")).thenReturn(ud);
        when(jwtUtil.validateToken("good.tok", "a@a.com")).thenReturn(true);

        filter.doFilterInternal(req, res, filterChain);

        verify(filterChain).doFilter(req, res);
        org.junit.jupiter.api.Assertions.assertNotNull(
                SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void blankAfterBearer_treatedAsNoToken() throws Exception {
        var req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer ");
        var res = new MockHttpServletResponse();
        filter.doFilterInternal(req, res, filterChain);
        verify(loginService, never()).loadUserByUsername(any());
        verify(filterChain).doFilter(req, res);
    }
}
