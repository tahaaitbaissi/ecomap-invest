package com.example.backend.events;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class ProfileEventPublisherTest {

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private ProfileEventPublisher profileEventPublisher;

    @Test
    void publishProfileGenerated_sendsEvent() {
        var pid = UUID.randomUUID();
        var uid = UUID.randomUUID();
        profileEventPublisher.publishProfileGenerated(pid, uid, "q1");
        var cap = ArgumentCaptor.forClass(ProfileGeneratedEvent.class);
        verify(applicationEventPublisher).publishEvent(cap.capture());
        ProfileGeneratedEvent e = cap.getValue();
        assertEquals(pid, e.getProfileId());
        assertEquals(uid, e.getUserId());
        assertEquals("q1", e.getQuery());
        assertNotNull(e.getGeneratedAt());
    }
}
