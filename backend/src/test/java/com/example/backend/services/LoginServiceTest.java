package com.example.backend.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.backend.entities.Role;
import com.example.backend.entities.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@ExtendWith(MockitoExtension.class)
class LoginServiceTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private LoginService loginService;

    @Test
    void loadUserByUsername_loadsFromUserService() {
        User u = new User();
        u.setEmail("e@e.com");
        u.setPassword("hash");
        u.setRole(Role.ROLE_INVESTOR.name());
        when(userService.getUserByEmail("e@e.com")).thenReturn(u);

        UserDetails d = loginService.loadUserByUsername("e@e.com");
        assertEquals("e@e.com", d.getUsername());
        assertEquals("hash", d.getPassword());
        assertNotNull(
                d.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .filter("ROLE_INVESTOR"::equals)
                        .findFirst()
                        .orElse(null));
        verify(userService).getUserByEmail("e@e.com");
    }

    @Test
    void loadUserByUsername_notFound() {
        when(userService.getUserByEmail("x@x.com"))
                .thenThrow(new UsernameNotFoundException("none"));
        assertThrows(UsernameNotFoundException.class, () -> loginService.loadUserByUsername("x@x.com"));
    }
}
