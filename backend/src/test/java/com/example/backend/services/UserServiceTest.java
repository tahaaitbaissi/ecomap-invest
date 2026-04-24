package com.example.backend.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.backend.controllers.dto.UpdateProfileRequest;
import com.example.backend.entities.User;
import com.example.backend.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void getUserByEmail_found() {
        User u = new User();
        u.setEmail("a@a.com");
        when(userRepository.findByEmail("a@a.com")).thenReturn(java.util.Optional.of(u));
        assertEquals(u, userService.getUserByEmail("a@a.com"));
    }

    @Test
    void getUserByEmail_missing() {
        when(userRepository.findByEmail("a@a.com")).thenReturn(java.util.Optional.empty());
        assertThrows(UsernameNotFoundException.class, () -> userService.getUserByEmail("a@a.com"));
    }

    @Test
    void updateProfile_usernameChange() {
        User u = new User();
        u.setId(1L);
        u.setEmail("a@a.com");
        u.setUsername("old");
        u.setName("N");
        when(userRepository.findByEmail("a@a.com")).thenReturn(java.util.Optional.of(u));
        when(userRepository.existsByUsername("new")).thenReturn(false);
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        User out = userService.updateProfile("a@a.com", new UpdateProfileRequest("new", null, null));
        assertEquals("new", out.getUsername());
    }

    @Test
    void updateProfile_noChanges_throws() {
        User u = new User();
        u.setEmail("a@a.com");
        u.setUsername("u");
        u.setName("N");
        when(userRepository.findByEmail("a@a.com")).thenReturn(java.util.Optional.of(u));
        assertThrows(
                IllegalArgumentException.class,
                () -> userService.updateProfile("a@a.com", new UpdateProfileRequest("u", "a@a.com", "N")));
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateProfile_usernameConflict() {
        User u = new User();
        u.setEmail("a@a.com");
        u.setUsername("u");
        u.setName("N");
        when(userRepository.findByEmail("a@a.com")).thenReturn(java.util.Optional.of(u));
        when(userRepository.existsByUsername("taken")).thenReturn(true);
        assertThrows(
                IllegalStateException.class,
                () -> userService.updateProfile("a@a.com", new UpdateProfileRequest("taken", null, null)));
    }
}
