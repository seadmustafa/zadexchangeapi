package com.zad.exchangeapi.service;

import com.zad.exchangeapi.config.ExchangeApiClient;
import com.zad.exchangeapi.entity.Currency;
import com.zad.exchangeapi.service.impl.ExchangeRateServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class ExchangeRateServiceImplTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ExchangeApiClient exchangeApiClient;

    @Mock
    private ValueOperations<String, String> valueOps;

    @InjectMocks
    private ExchangeRateServiceImpl service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
    }

    @Test
    void getExchangeRate_SameCurrency_ReturnsOne() {
        BigDecimal rate = service.getExchangeRate(Currency.USD, Currency.USD);
        assertThat(rate).isEqualTo(BigDecimal.ONE);
        verifyNoInteractions(redisTemplate, exchangeApiClient);
    }

    @Test
    void getExchangeRate_CacheHit_ReturnsCachedValue() {
        when(valueOps.get("rate:USD:TRY")).thenReturn("30.5");

        BigDecimal rate = service.getExchangeRate(Currency.USD, Currency.TRY);

        assertThat(rate).isEqualByComparingTo("30.5");
        verify(exchangeApiClient, never()).getExchangeRates(anyString());
    }

    @Test
    void getExchangeRate_CacheMiss_FetchesFromApiAndCaches() {
        when(valueOps.get("rate:USD:TRY")).thenReturn(null);
        when(exchangeApiClient.getExchangeRates("USD"))
                .thenReturn(Map.of("TRY", BigDecimal.valueOf(32.75)));

        BigDecimal rate = service.getExchangeRate(Currency.USD, Currency.TRY);

        assertThat(rate).isEqualByComparingTo("32.75");
        verify(exchangeApiClient).getExchangeRates("USD");
        verify(valueOps).set(eq("rate:USD:TRY"), eq("32.75"), any());
    }

    @Test
    void getExchangeRate_RateUnavailable_ThrowsException() {
        when(valueOps.get("rate:USD:TRY")).thenReturn(null);
        when(exchangeApiClient.getExchangeRates("USD")).thenReturn(Map.of());

        assertThatThrownBy(() -> service.getExchangeRate(Currency.USD, Currency.TRY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not available");
    }
}

