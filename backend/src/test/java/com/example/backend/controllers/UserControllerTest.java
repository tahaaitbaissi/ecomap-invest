package com.example.backend.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import com.example.backend.controllers.dto.UpdateProfileRequest;
import com.example.backend.entities.Role;
import com.example.backend.entities.User;
import com.example.backend.services.UserService;
import java.sql.Timestamp;
import java.util.UUID;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
/**
 * Direct controller invocations: {@code Authentication} in slice tests is awkward with Spring
 * Security 6 + WebMvc; behaviour is covered the same way.
 */
@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    @BeforeEach
    void clearMocks() {
        reset(userService);
    }

    private static UserDetails principal() {
        return org.springframework.security.core.userdetails.User.withUsername("a@a.com")
                .password("x")
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_INVESTOR")))
                .build();
    }

    @Test
    void getMyProfile_ok() {
        User u = new User();
        u.setId(UUID.randomUUID());
        u.setEmail("a@a.com");
        u.setCompanyName("Co");
        u.setRole(Role.ROLE_INVESTOR.name());
        u.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        when(userService.getUserByEmail("a@a.com")).thenReturn(u);

        ResponseEntity<?> res = userController.getMyProfile(principal());
        assertEquals(org.springframework.http.HttpStatus.OK, res.getStatusCode());
    }

    @Test
    void updateMyProfile_ok() {
        User u = new User();
        u.setId(UUID.randomUUID());
        u.setEmail("a@a.com");
        u.setCompanyName("NewCo");
        u.setRole(Role.ROLE_INVESTOR.name());
        u.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        when(userService.updateProfile(eq("a@a.com"), any(UpdateProfileRequest.class)))
                .thenReturn(u);
        var req = new UpdateProfileRequest("NewCo", "old", "NewPass123");
        ResponseEntity<?> res = userController.updateMyProfile(principal(), req);
        assertEquals(org.springframework.http.HttpStatus.OK, res.getStatusCode());
    }
}
