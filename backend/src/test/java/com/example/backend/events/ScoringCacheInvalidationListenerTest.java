package com.example.backend.events;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;

@ExtendWith(MockitoExtension.class)
class ScoringCacheInvalidationListenerTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private Cursor<String> cursor;

    @InjectMocks
    private ScoringCacheInvalidationListener listener;

    @Test
    void scansScoreKeysForProfileAfterCommitHandler() {
        UUID profileId = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
        when(stringRedisTemplate.scan(any(ScanOptions.class))).thenReturn(cursor);
        when(cursor.hasNext()).thenReturn(true, false);
        when(cursor.next()).thenReturn("score:" + profileId + ":99");
        when(stringRedisTemplate.delete(anyList())).thenReturn(1L);

        listener.onProfileGenerated(new ProfileGeneratedEvent(this, profileId, UUID.randomUUID(), "q", Instant.now()));

        verify(stringRedisTemplate).scan(any(ScanOptions.class));
        verify(stringRedisTemplate).delete(anyList());
    }
}
