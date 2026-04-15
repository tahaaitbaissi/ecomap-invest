package com.example.backend.events;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Removes cached hex scores for a profile when it is regenerated. Keys follow {@code score:{profileId}:*}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScoringCacheInvalidationListener {

    private static final int SCAN_COUNT = 200;
    private static final int DELETE_BATCH = 500;

    private final StringRedisTemplate stringRedisTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onProfileGenerated(ProfileGeneratedEvent event) {
        UUID profileId = event.getProfileId();
        String pattern = "score:" + profileId + ":*";
        ScanOptions options = ScanOptions.scanOptions().match(pattern).count(SCAN_COUNT).build();

        long deleted = 0;
        List<String> batch = new ArrayList<>();
        try (Cursor<String> cursor = stringRedisTemplate.scan(options)) {
            while (cursor.hasNext()) {
                batch.add(cursor.next());
                if (batch.size() >= DELETE_BATCH) {
                    Long n = stringRedisTemplate.delete(batch);
                    deleted += n != null ? n : 0;
                    batch.clear();
                }
            }
            if (!batch.isEmpty()) {
                Long n = stringRedisTemplate.delete(batch);
                deleted += n != null ? n : 0;
            }
        } catch (Exception e) {
            log.warn(
                    "Redis SCAN/delete failed for profile {} (cache may be empty or Redis down): {}",
                    profileId,
                    e.getMessage());
            return;
        }
        log.debug("Profile {} generated: invalidated {} score cache key(s)", profileId, deleted);
    }
}
