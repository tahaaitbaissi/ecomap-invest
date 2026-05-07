package com.example.backend.controllers;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.example.backend.controllers.dto.AiChatStreamRequest;
import com.example.backend.entities.DynamicProfile;
import com.example.backend.services.ai.AiChatService;
import com.example.backend.services.profile.DynamicProfileService;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@ExtendWith(MockitoExtension.class)
class AiChatControllerTest {

    @Mock
    private AiChatService aiChatService;

    @Mock
    private DynamicProfileService dynamicProfileService;

    private AiChatController controller;

    @BeforeEach
    void setUp() {
        controller = new AiChatController(aiChatService, dynamicProfileService);
        ReflectionTestUtils.setField(controller, "sseTimeoutMs", 60_000L);
        when(dynamicProfileService.getOwnedActiveEntity(any(), any(UUID.class))).thenReturn(new DynamicProfile());
    }

    @Test
    void chatStream_checksOwnership_andStartsStreaming() {
        UUID profileId = UUID.randomUUID();
        var auth = new UsernamePasswordAuthenticationToken(
                "a@a.com", null, java.util.List.of(new SimpleGrantedAuthority("ROLE_USER")));

        AiChatStreamRequest req =
                new AiChatStreamRequest("conv-1", profileId, "89283082803ffff", 42, "Pourquoi ce score ?");

        SseEmitter emitter = controller.chatStream(auth, req);
        assertNotNull(emitter);

        verify(dynamicProfileService).getOwnedActiveEntity("a@a.com", profileId);
        verify(aiChatService)
                .streamChatAsync(
                        eq("a@a.com"),
                        eq(profileId),
                        eq("89283082803ffff"),
                        eq(42),
                        eq("conv-1"),
                        eq("Pourquoi ce score ?"),
                        any(SseEmitter.class));
    }

    @Test
    void chatStream_ownershipFailureShortCircuits() {
        UUID profileId = UUID.randomUUID();
        var auth = new UsernamePasswordAuthenticationToken(
                "a@a.com", null, java.util.List.of(new SimpleGrantedAuthority("ROLE_USER")));
        when(dynamicProfileService.getOwnedActiveEntity("a@a.com", profileId))
                .thenThrow(new java.util.NoSuchElementException("gone"));

        org.junit.jupiter.api.Assertions.assertThrows(
                java.util.NoSuchElementException.class,
                () -> controller.chatStream(auth, new AiChatStreamRequest("c", profileId, "h", null, "m")));
        verifyNoInteractions(aiChatService);
    }
}

