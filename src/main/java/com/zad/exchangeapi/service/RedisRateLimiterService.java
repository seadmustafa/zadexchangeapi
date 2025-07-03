package com.zad.exchangeapi.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisRateLimiterService {

    private final StringRedisTemplate redisTemplate;

    // 10 requests allowed per minute per user
    private static final int MAX_REQUESTS = 10;
    private static final Duration WINDOW = Duration.ofMinutes(1);

    public boolean isAllowed(String userId) {
        String key = "RATE_LIMIT:" + userId;

        Long currentCount = redisTemplate.opsForValue().increment(key);
        if (currentCount != null && currentCount == 1) {
            redisTemplate.expire(key, WINDOW);
        }

        boolean allowed = currentCount != null && currentCount <= MAX_REQUESTS;

        if (!allowed) {
            log.warn("Rate limit exceeded for user {}", userId);
        }

        return allowed;
    }
}

