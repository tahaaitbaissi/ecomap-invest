package com.example.backend.bootstrap;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.backend.entities.User;
import com.example.backend.repositories.PoiRepository;
import com.example.backend.repositories.UserRepository;
import com.example.backend.services.CsvIngestionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class DataInitializerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private PoiRepository poiRepository;

    @Mock
    private CsvIngestionService csvIngestionService;

    private DataInitializer initializer;

    @BeforeEach
    void setUp() {
        initializer = new DataInitializer(
                userRepository, passwordEncoder, poiRepository, csvIngestionService);
    }

    @Test
    void run_createsUsersWhenMissing() throws Exception {
        when(userRepository.existsByEmail("admin@example.com")).thenReturn(false);
        when(userRepository.existsByEmail("user@example.com")).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("hash");
        when(poiRepository.count()).thenReturn(5L);

        initializer.run();

        var cap = ArgumentCaptor.forClass(User.class);
        verify(userRepository, org.mockito.Mockito.times(2)).save(cap.capture());
        assertTrue(
                cap.getAllValues().stream()
                        .anyMatch(u -> "admin@example.com".equals(u.getEmail())));
        assertTrue(
                cap.getAllValues().stream()
                        .anyMatch(u -> "user@example.com".equals(u.getEmail())));
        verify(csvIngestionService, never()).ingestFromCsv(any());
    }

    @Test
    void run_ingestsCsvWhenNoPois() throws Exception {
        when(userRepository.existsByEmail("admin@example.com")).thenReturn(true);
        when(userRepository.existsByEmail("user@example.com")).thenReturn(true);
        when(poiRepository.count()).thenReturn(0L);
        when(csvIngestionService.ingestFromCsv("data/mock_poi.csv")).thenReturn(7);
        initializer.run();
        verify(csvIngestionService).ingestFromCsv("data/mock_poi.csv");
    }
}
