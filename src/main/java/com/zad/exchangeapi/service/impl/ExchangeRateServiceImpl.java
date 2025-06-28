package com.zad.exchangeapi.service.impl;

import com.zad.exchangeapi.config.ExchangeApiClient;
import com.zad.exchangeapi.entity.Currency;
import com.zad.exchangeapi.service.ExchangeRateService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ExchangeRateServiceImpl implements ExchangeRateService {

    private static final Logger logger = LoggerFactory.getLogger(ExchangeRateServiceImpl.class);
    private static final String PREFIX = "rate:";

    private final StringRedisTemplate redisTemplate;
    private final ExchangeApiClient exchangeApiClient;

    @Override
    public BigDecimal getExchangeRate(Currency from, Currency to) {
        if (from == to) {
            return BigDecimal.ONE;
        }

        String key = buildCacheKey(from, to);
        String cachedRate = redisTemplate.opsForValue().get(key);

        if (cachedRate != null) {
            logger.info("Cache hit for rate {} -> {}", from, to);
            return new BigDecimal(cachedRate);
        }

        BigDecimal fetchedRate = fetchRateFromExternalApi(from, to);
        redisTemplate.opsForValue().set(key, fetchedRate.toPlainString(), Duration.ofMinutes(30));

        return fetchedRate;
    }

    private String buildCacheKey(Currency from, Currency to) {
        return PREFIX + from.name() + ":" + to.name();
    }

    private BigDecimal fetchRateFromExternalApi(Currency from, Currency to) {
        logger.warn("Fetching rate from external API for {} -> {}", from, to);

        Map<String, BigDecimal> rates = exchangeApiClient.getExchangeRates(from.name());
        BigDecimal rate = rates.get(to.name());

        if (rate == null) {
            throw new IllegalArgumentException("Exchange rate from " + from + " to " + to + " not available");
        }

        return rate;
    }
}
