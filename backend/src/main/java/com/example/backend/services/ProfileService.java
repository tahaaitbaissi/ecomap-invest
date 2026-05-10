package com.example.backend.services;

import com.example.backend.events.ProfileJmsPublisher;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Profile lifecycle hooks. Delegates to {@link ProfileJmsPublisher} while a DB transaction is active;
 * JMS is sent only after commit.
 */
@Service
@RequiredArgsConstructor
public class ProfileService {

    private final ProfileJmsPublisher profileJmsPublisher;

    @Transactional
    public void notifyProfileGeneratedAfterPersist(UUID profileId, UUID userId, String query) {
        profileJmsPublisher.publishProfileGenerated(profileId, userId, query);
    }
}
