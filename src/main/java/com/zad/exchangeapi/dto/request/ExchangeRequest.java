package com.zad.exchangeapi.dto.request;

import com.zad.exchangeapi.entity.Currency;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ExchangeRequest(
        @NotNull Long fromUserId,
        @NotNull Long toUserId,
        @NotNull Currency fromCurrency,
        @NotNull Currency toCurrency,
        @NotNull @Min(0) BigDecimal amount
) {
}