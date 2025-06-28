package com.zad.exchangeapi.service;


import com.zad.exchangeapi.dto.kafka.DepositMessage;
import com.zad.exchangeapi.dto.kafka.WithdrawMessage;
import com.zad.exchangeapi.entity.Account;
import com.zad.exchangeapi.entity.Currency;
import com.zad.exchangeapi.entity.User;
import com.zad.exchangeapi.service.messaging.consumer.TransactionConsumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;

import static org.mockito.Mockito.*;

class TransactionConsumerTest {

    @Mock
    private AccountService accountService;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private TransactionConsumer consumer;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void handleDeposit_ShouldUpdateBalanceAndSetSuccessStatus() {
        DepositMessage message = new DepositMessage(1L, Currency.USD, new BigDecimal("100"), "op-123");
        User mockUser = new User();
        Account account = new Account(1L, Currency.USD, mockUser, new BigDecimal("200"));

        when(accountService.getAccount(1L, Currency.USD)).thenReturn(account);

        consumer.handleDeposit(message);

        verify(accountService).updateBalance(account, new BigDecimal("300"));
        verify(valueOperations).set(eq("result:op-123"), eq("SUCCESS"), any());
    }

    @Test
    void handleDeposit_ShouldSetFailureStatusOnException() {
        DepositMessage message = new DepositMessage(1L, Currency.USD, new BigDecimal("100"), "op-999");

        when(accountService.getAccount(1L, Currency.USD)).thenThrow(new RuntimeException("Test error"));

        consumer.handleDeposit(message);

        verify(valueOperations).set(contains("result:op-999"), contains("FAILURE: Test error"), any());
    }

    @Test
    void handleWithdraw_ShouldUpdateBalanceAndSetSuccessStatus() {
        WithdrawMessage message = new WithdrawMessage(1L, Currency.USD, new BigDecimal("50"), "op-222");
        User mockUser = new User();
        Account account = new Account(1L, Currency.USD, mockUser, new BigDecimal("100"));

        when(accountService.getAccount(1L, Currency.USD)).thenReturn(account);

        consumer.handleWithdraw(message);

        verify(accountService).updateBalance(account, new BigDecimal("50"));
        verify(valueOperations).set(eq("result:op-222"), eq("SUCCESS"), any());
    }

    @Test
    void handleWithdraw_ShouldSetFailureStatusOnException() {
        WithdrawMessage message = new WithdrawMessage(1L, Currency.USD, new BigDecimal("50"), "op-333");

        when(accountService.getAccount(1L, Currency.USD)).thenThrow(new RuntimeException("Withdraw error"));

        consumer.handleWithdraw(message);

        verify(valueOperations).set(contains("result:op-333"), contains("FAILURE: Withdraw error"), any());
    }
}


