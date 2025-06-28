package com.zad.exchangeapi.utils;

import com.zad.exchangeapi.service.messaging.producer.TransactionProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionRetryScheduler {

    private final RedisTemplate<String, String> redisTemplate;
    private final TransactionProducer producer;

    @Scheduled(fixedRate = 180_000) // Every 3 min
    public void retryPendingTransactions() {
        log.info("Running retry job for pending transactions");

        // Example for demo - production: Use SCAN or Redis Streams for scalability
        Set<String> keys = redisTemplate.keys("result:*");

        keys.forEach(key -> {
            String status = redisTemplate.opsForValue().get(key);
            if ("PENDING".equals(status)) {
                log.warn("Retrying transaction: {}", key);
                String[] parts = key.split(":");
                Long userId = Long.valueOf(parts[1]);
                String transactionId = parts[2];

                // Example re-publish (mocked)
                producer.retryTransaction(userId, transactionId);
            }
        });
    }
}
