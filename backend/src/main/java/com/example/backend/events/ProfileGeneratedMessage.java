package com.example.backend.events;

import java.time.Instant;
import java.util.UUID;

/** JMS payload when a dynamic profile is persisted (JSON over JMS). */
public record ProfileGeneratedMessage(UUID profileId, UUID userId, String query, Instant generatedAt) {}
