package com.example.backend.controllers;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.example.backend.entities.DynamicProfile;
import com.example.backend.entities.DynamicProfile;
import com.example.backend.services.explain.HexScoreExplanationService;
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
class AiExplainControllerTest {

    @Mock
    private HexScoreExplanationService hexScoreExplanationService;

    @Mock
    private DynamicProfileService dynamicProfileService;

    private AiExplainController controller;

    @BeforeEach
    void setUp() {
        controller = new AiExplainController(hexScoreExplanationService, dynamicProfileService);
        ReflectionTestUtils.setField(controller, "sseTimeoutMs", 60_000L);
        when(dynamicProfileService.getOwnedActiveEntity(any(), any(UUID.class))).thenReturn(new DynamicProfile());
    }

    @Test
    void explainHexScore_checksOwnership_andStartsStreaming() {
        UUID profileId = UUID.randomUUID();
        var auth = new UsernamePasswordAuthenticationToken(
                "a@a.com", null, java.util.List.of(new SimpleGrantedAuthority("ROLE_USER")));
        String h3 = "89283082803ffff";

        SseEmitter emitter = controller.explainHexScore(auth, h3, profileId, null);
        assertNotNull(emitter);
        verify(dynamicProfileService).getOwnedActiveEntity("a@a.com", profileId);
        verify(hexScoreExplanationService).streamExplanationAsync(eq(profileId), eq(h3), isNull(), any(SseEmitter.class));
    }

    @Test
    void explainHexScore_passesViewportCount() {
        UUID profileId = UUID.randomUUID();
        var auth = new UsernamePasswordAuthenticationToken(
                "a@a.com", null, java.util.List.of(new SimpleGrantedAuthority("ROLE_USER")));

        controller.explainHexScore(auth, "89283082803ffff", profileId, 42);
        verify(hexScoreExplanationService).streamExplanationAsync(eq(profileId), eq("89283082803ffff"), eq(42), any());
    }

    @Test
    void explainHexScore_ownershipFailureShortCircuits() {
        UUID profileId = UUID.randomUUID();
        var auth = new UsernamePasswordAuthenticationToken(
                "a@a.com", null, java.util.List.of(new SimpleGrantedAuthority("ROLE_USER")));
        when(dynamicProfileService.getOwnedActiveEntity("a@a.com", profileId))
                .thenThrow(new java.util.NoSuchElementException("gone"));

        org.junit.jupiter.api.Assertions.assertThrows(
                java.util.NoSuchElementException.class,
                () -> controller.explainHexScore(auth, "89283082803ffff", profileId, null));
        verifyNoInteractions(hexScoreExplanationService);
    }
}
