package com.zad.exchangeapi.service;

import com.zad.exchangeapi.dto.request.DepositRequest;
import com.zad.exchangeapi.dto.request.ExchangeRequest;
import com.zad.exchangeapi.dto.request.WithdrawRequest;
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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class TransactionServiceImplTest {

    @InjectMocks
    private TransactionServiceImpl transactionService;

    @Mock
    private UserService userService;

    @Mock
    private AccountService accountService;

    @Mock
    private ExchangeRateService exchangeRateService;

    private final User mockUser = new User(1L, "testUser", null);

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void deposit_validRequest_shouldUpdateBalance() {
        Account account = new Account(10L, Currency.USD, mockUser, new BigDecimal("100"));
        DepositRequest request = new DepositRequest(1L, Currency.USD, new BigDecimal("50"));

        when(accountService.getAccount(1L, Currency.USD)).thenReturn(account);

        GenericResponse response = transactionService.deposit(request);

        assertTrue(response.success());
        verify(accountService).updateBalance(eq(account), eq(new BigDecimal("150")));
    }

    @Test
    void exchange_valid_shouldUpdateBothAccounts() {
        Account from = new Account(1L, Currency.USD, mockUser, new BigDecimal("100"));
        Account to = new Account(2L, Currency.TRY, mockUser, new BigDecimal("500"));
        ExchangeRequest request = new ExchangeRequest(1L, 2L, Currency.USD, Currency.TRY, new BigDecimal("50"));

        when(accountService.getAccount(1L, Currency.USD)).thenReturn(from);
        when(accountService.getAccount(2L, Currency.TRY)).thenReturn(to);
        when(exchangeRateService.getExchangeRate(Currency.USD, Currency.TRY)).thenReturn(new BigDecimal("30"));

        GenericResponse response = transactionService.exchange(request);

        assertTrue(response.success());
        verify(accountService).updateBalance(from, new BigDecimal("50"));
        verify(accountService).updateBalance(to, new BigDecimal("2000"));
    }
}
