package com.zad.exchangeapi.dto.response;

import com.zad.exchangeapi.entity.Currency;

import java.math.BigDecimal;

public record BalanceResponse(
        Long userId,
        Currency currency,
        BigDecimal balance
) {
}