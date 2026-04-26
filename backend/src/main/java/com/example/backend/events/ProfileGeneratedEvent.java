package com.example.backend.events;

import java.time.Instant;
import java.util.UUID;
import org.springframework.context.ApplicationEvent;

public class ProfileGeneratedEvent extends ApplicationEvent {

    private final UUID profileId;
    private final UUID userId;
    private final String query;
    private final Instant generatedAt;

    public ProfileGeneratedEvent(Object source, UUID profileId, UUID userId, String query, Instant generatedAt) {
        super(source);
        this.profileId = profileId;
        this.userId = userId;
        this.query = query;
        this.generatedAt = generatedAt;
    }

    public UUID getProfileId() {
        return profileId;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getQuery() {
        return query;
    }

    public Instant getGeneratedAt() {
        return generatedAt;
    }
}
