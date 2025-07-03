package com.zad.exchangeapi.utils;

import com.zad.exchangeapi.service.messaging.producer.TransactionProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Set;

import static org.mockito.Mockito.*;

class TransactionRetrySchedulerTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private TransactionProducer transactionProducer;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private TransactionRetryScheduler retryScheduler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void retryPendingTransactions_ShouldRetryOnlyPendingTransactions() {

        Set<String> keys = Set.of(
                "result:55:txn-111",
                "result:77:txn-222",
                "result:88:txn-333"
        );

        when(redisTemplate.keys("result:*")).thenReturn(keys);
        when(valueOperations.get("result:55:txn-111")).thenReturn("PENDING");
        when(valueOperations.get("result:77:txn-222")).thenReturn("SUCCESS");
        when(valueOperations.get("result:88:txn-333")).thenReturn("PENDING");

        retryScheduler.retryPendingTransactions();

        // Only pending transactions should trigger retry
        verify(transactionProducer).retryTransaction(55L, "txn-111");
        verify(transactionProducer).retryTransaction(88L, "txn-333");

        // Success transaction should be skipped
        verify(transactionProducer, times(2)).retryTransaction(anyLong(), anyString());
    }

    @Test
    void retryPendingTransactions_ShouldHandleNoKeysGracefully() {
        when(redisTemplate.keys("result:*")).thenReturn(Set.of());

        retryScheduler.retryPendingTransactions();

        verifyNoInteractions(transactionProducer);
    }


}
