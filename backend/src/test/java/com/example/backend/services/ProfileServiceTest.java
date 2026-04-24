package com.example.backend.services;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import com.example.backend.events.ProfileEventPublisher;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock
    private ProfileEventPublisher profileEventPublisher;

    @InjectMocks
    private ProfileService profileService;

    @Test
    void notifyProfileGeneratedAfterPersist_publishes() {
        UUID pid = UUID.randomUUID();
        profileService.notifyProfileGeneratedAfterPersist(pid, 1L, "q");
        verify(profileEventPublisher)
                .publishProfileGenerated(
                        eq(pid), eq(1L), eq("q"));
    }
}
