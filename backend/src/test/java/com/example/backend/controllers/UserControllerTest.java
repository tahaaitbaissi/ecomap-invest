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
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
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

    private static UsernamePasswordAuthenticationToken auth() {
        return new UsernamePasswordAuthenticationToken(
                "a@a.com", "x", List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    @Test
    void getMyProfile_ok() {
        User u = new User();
        u.setId(1L);
        u.setEmail("a@a.com");
        u.setUsername("u1");
        u.setName("N");
        u.setRole(Role.USER);
        when(userService.getUserByEmail("a@a.com")).thenReturn(u);

        ResponseEntity<?> res = userController.getMyProfile(auth());
        assertEquals(HttpStatus.OK, res.getStatusCode());
    }

    @Test
    void updateMyProfile_ok() {
        User u = new User();
        u.setId(1L);
        u.setEmail("a@a.com");
        u.setUsername("user2");
        u.setName("NewName");
        u.setRole(Role.USER);
        when(userService.updateProfile(eq("a@a.com"), any(UpdateProfileRequest.class)))
                .thenReturn(u);
        var req = new UpdateProfileRequest("user2", "a@a.com", "NewName");
        ResponseEntity<?> res = userController.updateMyProfile(auth(), req);
        assertEquals(HttpStatus.OK, res.getStatusCode());
    }

    @Test
    void updateMyProfile_conflict() {
        when(userService.updateProfile(eq("a@a.com"), any(UpdateProfileRequest.class)))
                .thenThrow(new IllegalStateException("taken"));
        var body = new UpdateProfileRequest("taken", "a@a.com", "Na");
        ResponseEntity<?> res = userController.updateMyProfile(auth(), body);
        assertEquals(HttpStatus.CONFLICT, res.getStatusCode());
    }
}
