package com.example.backend.events;

import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProfileEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    /**
     * Call after a dynamic profile is persisted (F7). Triggers cache invalidation listeners.
     */
    public void publishProfileGenerated(UUID profileId, UUID userId, String query) {
        applicationEventPublisher.publishEvent(
                new ProfileGeneratedEvent(this, profileId, userId, query, Instant.now()));
    }
}
