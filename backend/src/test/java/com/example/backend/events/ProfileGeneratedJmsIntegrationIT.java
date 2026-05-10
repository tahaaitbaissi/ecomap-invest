package com.example.backend.events;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.backend.BackendApplication;
import com.example.backend.testsupport.AbstractPostgisRedisIntegrationTest;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.context.TestPropertySource;

/**
 * End-to-end JMS: message hits Artemis (Testcontainers), listener clears Redis score keys for the
 * profile.
 */
@SpringBootTest(classes = BackendApplication.class)
@Tag("integration")
@TestPropertySource(
        properties = {
            "app.jms.queue.profile-generated=profile.generated",
        })
class ProfileGeneratedJmsIntegrationIT extends AbstractPostgisRedisIntegrationTest {

    private static final String QUEUE = "profile.generated";

    @Autowired
    private JmsTemplate jmsTemplate;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Test
    void jmsListenerInvalidatesScoreCacheKeys() throws Exception {
        UUID profileId = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
        UUID userId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        String key = "score:v3:9:" + profileId + ":42";
        stringRedisTemplate.opsForValue().set(key, "1");
        assertTrue(Boolean.TRUE.equals(stringRedisTemplate.hasKey(key)));

        ProfileGeneratedMessage msg = new ProfileGeneratedMessage(profileId, userId, "it-query", Instant.now());
        jmsTemplate.convertAndSend(QUEUE, msg);

        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(15);
        while (System.nanoTime() < deadline) {
            if (!Boolean.TRUE.equals(stringRedisTemplate.hasKey(key))) {
                break;
            }
            Thread.sleep(80);
        }
        assertFalse(Boolean.TRUE.equals(stringRedisTemplate.hasKey(key)));
    }
}
