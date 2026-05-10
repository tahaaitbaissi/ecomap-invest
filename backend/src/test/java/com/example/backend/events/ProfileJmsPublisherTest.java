package com.example.backend.events;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@ExtendWith(MockitoExtension.class)
class ProfileJmsPublisherTest {

    private static final String QUEUE = "profile.generated";

    @Mock
    private JmsTemplate jmsTemplate;

    @InjectMocks
    private ProfileJmsPublisher profileJmsPublisher;

    @BeforeEach
    void setQueueName() {
        ReflectionTestUtils.setField(profileJmsPublisher, "profileGeneratedQueue", QUEUE);
    }

    @Test
    void publishProfileGenerated_whenNoTransaction_sendsJmsImmediately() {
        var pid = UUID.randomUUID();
        var uid = UUID.randomUUID();
        try (var tx = mockStatic(TransactionSynchronizationManager.class)) {
            tx.when(TransactionSynchronizationManager::isSynchronizationActive).thenReturn(false);
            profileJmsPublisher.publishProfileGenerated(pid, uid, "q1");
        }
        var cap = ArgumentCaptor.forClass(ProfileGeneratedMessage.class);
        verify(jmsTemplate).convertAndSend(eq(QUEUE), cap.capture());
        ProfileGeneratedMessage m = cap.getValue();
        assertEquals(pid, m.profileId());
        assertEquals(uid, m.userId());
        assertEquals("q1", m.query());
    }

    @Test
    void publishProfileGenerated_whenTransactionActive_sendsOnlyAfterCommit() {
        var pid = UUID.randomUUID();
        var uid = UUID.randomUUID();
        try (var tx = mockStatic(TransactionSynchronizationManager.class)) {
            tx.when(TransactionSynchronizationManager::isSynchronizationActive).thenReturn(true);
            profileJmsPublisher.publishProfileGenerated(pid, uid, "q2");
            ArgumentCaptor<TransactionSynchronization> syncCap = ArgumentCaptor.forClass(TransactionSynchronization.class);
            tx.verify(() -> TransactionSynchronizationManager.registerSynchronization(syncCap.capture()), times(1));
            verify(jmsTemplate, never()).convertAndSend(anyString(), isA(ProfileGeneratedMessage.class));
            syncCap.getValue().afterCommit();
            verify(jmsTemplate).convertAndSend(eq(QUEUE), isA(ProfileGeneratedMessage.class));
        }
    }
}
