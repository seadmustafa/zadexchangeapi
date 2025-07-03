package com.zad.exchangeapi.service;

import com.zad.exchangeapi.dto.request.DepositRequest;
import com.zad.exchangeapi.dto.request.ExchangeRequest;
import com.zad.exchangeapi.dto.request.WithdrawRequest;
import com.zad.exchangeapi.dto.response.BalanceResponse;
import com.zad.exchangeapi.dto.response.GenericResponse;
import com.zad.exchangeapi.entity.Account;
import com.zad.exchangeapi.entity.Currency;
import com.zad.exchangeapi.entity.User;
import com.zad.exchangeapi.service.impl.TransactionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TransactionServiceImplTest {

    @Mock
    private UserService userService;

    @Mock
    private AccountService accountService;

    @Mock
    private ExchangeRateService exchangeRateService;

    @InjectMocks
    private TransactionServiceImpl transactionService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void deposit_ShouldUpdateBalanceSuccessfully() {
        DepositRequest request = new DepositRequest(1L, Currency.USD, BigDecimal.valueOf(100));
        Account account = new Account(1L, Currency.USD, new User(), BigDecimal.valueOf(200));

        when(accountService.getAccount(1L, Currency.USD)).thenReturn(account);

        GenericResponse response = transactionService.deposit(request);

        assertThat(response.success()).isTrue();
        verify(accountService).updateBalance(account, BigDecimal.valueOf(300));
    }

    @Test
    void deposit_ShouldThrowForInvalidAmount() {
        DepositRequest request = new DepositRequest(1L, Currency.USD, BigDecimal.ZERO);

        assertThatThrownBy(() -> transactionService.deposit(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Amount must be greater than zero");
    }

    @Test
    void withdraw_ShouldUpdateBalanceSuccessfully() {
        WithdrawRequest request = new WithdrawRequest(1L, Currency.USD, BigDecimal.valueOf(50));
        Account account = new Account(1L, Currency.USD, new User(), BigDecimal.valueOf(100));

        when(accountService.getAccount(1L, Currency.USD)).thenReturn(account);

        GenericResponse response = transactionService.withdraw(request);

        assertThat(response.success()).isTrue();
        verify(accountService).validateSufficientBalance(account, BigDecimal.valueOf(50));
        verify(accountService).updateBalance(account, BigDecimal.valueOf(50));
    }

    @Test
    void withdraw_ShouldThrowForInvalidAmount() {
        WithdrawRequest request = new WithdrawRequest(1L, Currency.USD, BigDecimal.valueOf(-10));

        assertThatThrownBy(() -> transactionService.withdraw(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Amount must be greater than zero");
    }

    @Test
    void exchange_ShouldPerformCurrencyExchange() {
        ExchangeRequest request = new ExchangeRequest(1L, 2L, Currency.USD, Currency.TRY, BigDecimal.valueOf(100));
        Account fromAccount = new Account(1L, Currency.USD, new User(), BigDecimal.valueOf(500));
        Account toAccount = new Account(2L, Currency.TRY, new User(), BigDecimal.valueOf(1000));

        when(accountService.getAccount(1L, Currency.USD)).thenReturn(fromAccount);
        when(accountService.getAccount(2L, Currency.TRY)).thenReturn(toAccount);
        when(exchangeRateService.getExchangeRate(Currency.USD, Currency.TRY)).thenReturn(BigDecimal.valueOf(30));

        GenericResponse response = transactionService.exchange(request);

        assertThat(response.success()).isTrue();
        verify(accountService).validateSufficientBalance(fromAccount, BigDecimal.valueOf(100));
        verify(accountService).updateBalance(fromAccount, BigDecimal.valueOf(400));
        verify(accountService).updateBalance(toAccount, BigDecimal.valueOf(4000));
    }

    @Test
    void exchange_ShouldThrowForSameCurrency() {
        ExchangeRequest request = new ExchangeRequest(1L, 2L, Currency.USD, Currency.USD, BigDecimal.valueOf(50));

        assertThatThrownBy(() -> transactionService.exchange(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Source and target currencies must be different");
    }

    @Test
    void getBalance_ShouldReturnUserBalance() {
        Account account = new Account(1L, Currency.USD, new User(), BigDecimal.valueOf(500));
        when(accountService.getAccount(1L, Currency.USD)).thenReturn(account);

        BalanceResponse response = transactionService.getBalance(1L);

        assertThat(response.userId()).isEqualTo(1L);
        assertThat(response.balance()).isEqualTo(BigDecimal.valueOf(500));
    }
}
