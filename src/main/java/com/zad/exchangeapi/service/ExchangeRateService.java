package com.zad.exchangeapi.service;


import com.zad.exchangeapi.entity.Currency;

import java.math.BigDecimal;

public interface ExchangeRateService {
    BigDecimal getExchangeRate(Currency from, Currency to);
}
