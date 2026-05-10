package com.example.backend.events;

import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Publishes {@link ProfileGeneratedMessage} to Artemis <strong>after</strong> the DB transaction
 * commits (same semantics as the former {@code @TransactionalEventListener(AFTER_COMMIT)}).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProfileJmsPublisher {

    private final JmsTemplate jmsTemplate;

    @Value("${app.jms.queue.profile-generated}")
    private String profileGeneratedQueue;

    /**
     * Schedule a JMS send after successful commit. Call only from code running inside a transaction
     * after the entity is persisted.
     */
    public void publishProfileGenerated(UUID profileId, UUID userId, String query) {
        ProfileGeneratedMessage payload =
                new ProfileGeneratedMessage(profileId, userId, query, Instant.now());
        Runnable send = () -> sendOrLog(payload);

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    send.run();
                }
            });
        } else {
            log.warn(
                    "No active transaction when publishing profile {} — sending JMS immediately (unexpected in normal flows)",
                    profileId);
            send.run();
        }
    }

    private void sendOrLog(ProfileGeneratedMessage payload) {
        try {
            jmsTemplate.convertAndSend(profileGeneratedQueue, payload);
        } catch (Exception e) {
            log.error(
                    "Failed to send ProfileGeneratedMessage for profile {}: {}",
                    payload.profileId(),
                    e.getMessage(),
                    e);
        }
    }
}
