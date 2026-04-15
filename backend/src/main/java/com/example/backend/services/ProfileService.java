package com.example.backend.services;

import com.example.backend.events.ProfileEventPublisher;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Profile lifecycle hooks. Publish domain events from here inside a transaction so
 * {@link org.springframework.transaction.event.TransactionalEventListener} subscribers run after commit.
 */
@Service
@RequiredArgsConstructor
public class ProfileService {

    private final ProfileEventPublisher profileEventPublisher;

    @Transactional
    public void notifyProfileGeneratedAfterPersist(UUID profileId, Long userId, String query) {
        profileEventPublisher.publishProfileGenerated(profileId, userId, query);
    }
}
