package com.zad.exchangeapi.service.impl;

import com.zad.exchangeapi.dto.request.DepositRequest;
import com.zad.exchangeapi.dto.request.ExchangeRequest;
import com.zad.exchangeapi.dto.request.WithdrawRequest;
import com.zad.exchangeapi.dto.response.BalanceResponse;
import com.zad.exchangeapi.dto.response.GenericResponse;
import com.zad.exchangeapi.entity.Account;
import com.zad.exchangeapi.entity.Currency;
import com.zad.exchangeapi.service.AccountService;
import com.zad.exchangeapi.service.ExchangeRateService;
import com.zad.exchangeapi.service.TransactionService;
import com.zad.exchangeapi.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final UserService userService;
    private final AccountService accountService;
    private final ExchangeRateService exchangeRateService;

    @Override
    @Transactional
    public GenericResponse deposit(DepositRequest request) {
        validateAmount(request.amount());
        Account account = accountService.getAccount(request.userId(), request.currency());
        BigDecimal updated = account.getBalance().add(request.amount());
        accountService.updateBalance(account, updated);
        return new GenericResponse(true, "Deposit successful");
    }

    @Override
    @Transactional
    public GenericResponse withdraw(WithdrawRequest request) {
        validateAmount(request.amount());
        Account account = accountService.getAccount(request.userId(), request.currency());
        accountService.validateSufficientBalance(account, request.amount());
        BigDecimal updated = account.getBalance().subtract(request.amount());
        accountService.updateBalance(account, updated);
        return new GenericResponse(true, "Withdrawal successful");
    }

    @Override
    @Transactional
    public GenericResponse exchange(ExchangeRequest request) {
        validateAmount(request.amount());
        if (request.fromCurrency().equals(request.toCurrency())) {
            throw new IllegalArgumentException("Source and target currencies must be different");
        }

        Account fromAccount = accountService.getAccount(request.fromUserId(), request.fromCurrency());
        Account toAccount = accountService.getAccount(request.toUserId(), request.toCurrency());

        accountService.validateSufficientBalance(fromAccount, request.amount());

        BigDecimal rate = exchangeRateService.getExchangeRate(request.fromCurrency(), request.toCurrency());
        BigDecimal targetAmount = request.amount().multiply(rate);

        accountService.updateBalance(fromAccount, fromAccount.getBalance().subtract(request.amount()));
        accountService.updateBalance(toAccount, toAccount.getBalance().add(targetAmount));

        return new GenericResponse(true, "Exchange completed successfully");
    }

    @Override
    public BalanceResponse getBalance(Long userId) {
        Account account = accountService.getAccount(userId, Currency.USD);
        return new BalanceResponse(userId, Currency.USD, account.getBalance());
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
    }
}
