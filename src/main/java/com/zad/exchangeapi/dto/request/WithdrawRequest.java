package com.zad.exchangeapi.dto.request;

import com.zad.exchangeapi.entity.Currency;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record WithdrawRequest(
        @NotNull Long userId,
        @NotNull Currency currency,
        @NotNull @Min(0) BigDecimal amount
) {
}