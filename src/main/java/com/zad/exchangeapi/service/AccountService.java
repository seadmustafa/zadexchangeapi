package com.zad.exchangeapi.service;


import com.zad.exchangeapi.entity.Account;
import com.zad.exchangeapi.entity.Currency;

import java.math.BigDecimal;

public interface AccountService {
    Account getAccount(Long userId, Currency currency);

    void updateBalance(Account account, BigDecimal newBalance);

    void validateSufficientBalance(Account account, BigDecimal amount);
}
