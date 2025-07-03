package com.zad.exchangeapi.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class RedisRateLimiterServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private RedisRateLimiterService rateLimiterService;

    private static final String USER_ID = "test-user";
    private static final String REDIS_KEY = "RATE_LIMIT:" + USER_ID;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void isAllowed_ShouldAllowFirstRequestAndSetExpiry() {
        when(valueOperations.increment(REDIS_KEY)).thenReturn(1L);

        boolean allowed = rateLimiterService.isAllowed(USER_ID);

        assertThat(allowed).isTrue();
        verify(redisTemplate).expire(eq(REDIS_KEY), eq(Duration.ofMinutes(1)));
    }

    @Test
    void isAllowed_ShouldAllowRequestsUnderLimit() {
        when(valueOperations.increment(REDIS_KEY)).thenReturn(5L);

        boolean allowed = rateLimiterService.isAllowed(USER_ID);

        assertThat(allowed).isTrue();
        verify(redisTemplate, never()).expire(anyString(), any());
    }

    @Test
    void isAllowed_ShouldRejectRequestsOverLimit() {
        when(valueOperations.increment(REDIS_KEY)).thenReturn(11L);

        boolean allowed = rateLimiterService.isAllowed(USER_ID);

        assertThat(allowed).isFalse();
        verify(redisTemplate, never()).expire(anyString(), any());
    }

    @Test
    void isAllowed_ShouldRejectIfIncrementReturnsNull() {
        when(valueOperations.increment(REDIS_KEY)).thenReturn(null);

        boolean allowed = rateLimiterService.isAllowed(USER_ID);

        assertThat(allowed).isFalse();
        verify(redisTemplate, never()).expire(anyString(), any());
    }
}
