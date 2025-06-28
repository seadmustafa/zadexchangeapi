package com.zad.exchangeapi.service.impl;


import com.zad.exchangeapi.entity.Account;
import com.zad.exchangeapi.entity.Currency;
import com.zad.exchangeapi.repository.AccountRepository;
import com.zad.exchangeapi.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;

    @Override
    public Account getAccount(Long userId, Currency currency) {
        return accountRepository.findByUserIdAndCurrency(userId, currency)
                .orElseThrow(() -> new com.example.balanceapi.exception.NotFoundExceptionn("Account not found for user " + userId + " and currency " + currency));
    }

    @Override
    @Transactional
    public void updateBalance(Account account, BigDecimal newBalance) {
        if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Balance cannot be negative");
        }
        account.setBalance(newBalance);
        accountRepository.save(account);
    }

    @Override
    public void validateSufficientBalance(Account account, BigDecimal amount) {
        if (account.getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient balance in account");
        }
    }
}
