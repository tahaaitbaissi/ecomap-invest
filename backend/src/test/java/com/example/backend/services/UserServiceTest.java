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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UserService userService() {
        return new UserService(userRepository, passwordEncoder);
    }

    @Test
    void getUserByEmail_found() {
        User u = new User();
        u.setEmail("a@a.com");
        when(userRepository.findByEmail("a@a.com")).thenReturn(java.util.Optional.of(u));
        assertEquals(u, userService().getUserByEmail("a@a.com"));
    }

    @Test
    void getUserByEmail_missing() {
        when(userRepository.findByEmail("a@a.com")).thenReturn(java.util.Optional.empty());
        assertThrows(UsernameNotFoundException.class, () -> userService().getUserByEmail("a@a.com"));
    }

    @Test
    void updateProfile_companyNameChange() {
        User u = new User();
        u.setEmail("a@a.com");
        when(userRepository.findByEmail("a@a.com")).thenReturn(java.util.Optional.of(u));
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        User out = userService().updateProfile("a@a.com", new UpdateProfileRequest("NewCo", null, null));
        assertEquals("NewCo", out.getCompanyName());
    }

    @Test
    void updateProfile_noChanges_throws() {
        User u = new User();
        u.setEmail("a@a.com");
        when(userRepository.findByEmail("a@a.com")).thenReturn(java.util.Optional.of(u));
        assertThrows(
                IllegalArgumentException.class,
                () -> userService().updateProfile("a@a.com", new UpdateProfileRequest(null, null, null)));
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateProfile_passwordChange_requiresCurrentPassword() {
        User u = new User();
        u.setPassword("hashed");
        u.setEmail("a@a.com");
        when(userRepository.findByEmail("a@a.com")).thenReturn(java.util.Optional.of(u));
        assertThrows(
                IllegalArgumentException.class,
                () -> userService().updateProfile("a@a.com", new UpdateProfileRequest(null, null, "NewPass123")));
    }
}
