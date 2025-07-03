package com.zad.exchangeapi.service;


import com.zad.exchangeapi.dto.kafka.DepositMessage;
import com.zad.exchangeapi.dto.kafka.WithdrawMessage;
import com.zad.exchangeapi.entity.Account;
import com.zad.exchangeapi.entity.Currency;
import com.zad.exchangeapi.entity.OperationType;
import com.zad.exchangeapi.entity.User;
import com.zad.exchangeapi.service.messaging.consumer.TransactionConsumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
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
        DepositMessage message = DepositMessage.builder()
                .userId(1L)
                .currency(Currency.USD)
                .amount(BigDecimal.valueOf(100))
                .operationId("op-123")
                .operationType(OperationType.DEPOSIT)
                .build();

        User user = new User();
        Account account = new Account(1L, Currency.USD, user, BigDecimal.valueOf(200));

        when(accountService.getAccount(1L, Currency.USD)).thenReturn(account);

        consumer.handleDeposit(message);

        verify(accountService).updateBalance(account, BigDecimal.valueOf(300));
        verify(valueOperations).set(eq("result:op-123"), eq("SUCCESS"), eq(Duration.ofMinutes(10)));
    }

    @Test
    void handleDeposit_ShouldSetFailureStatus_OnException() {
        DepositMessage message = DepositMessage.builder()
                .userId(1L)
                .currency(Currency.USD)
                .amount(BigDecimal.valueOf(100))
                .operationId("op-999")
                .operationType(OperationType.DEPOSIT)
                .build();

        when(accountService.getAccount(1L, Currency.USD)).thenThrow(new RuntimeException("Simulated error"));

        consumer.handleDeposit(message);

        verify(valueOperations).set(contains("result:op-999"), contains("FAILURE: Simulated error"), eq(Duration.ofMinutes(10)));
    }

    @Test
    void handleWithdraw_ShouldUpdateBalanceAndSetSuccessStatus() {
        WithdrawMessage message = WithdrawMessage.builder()
                .userId(1L)
                .currency(Currency.USD)
                .amount(BigDecimal.valueOf(50))
                .operationId("op-222")
                .operationType(OperationType.WITHDRAW)
                .build();

        User user = new User();
        Account account = new Account(1L, Currency.USD, user, BigDecimal.valueOf(100));

        when(accountService.getAccount(1L, Currency.USD)).thenReturn(account);

        consumer.handleWithdraw(message);

        verify(accountService).validateSufficientBalance(account, BigDecimal.valueOf(50));
        verify(accountService).updateBalance(account, BigDecimal.valueOf(50));
        verify(valueOperations).set(eq("result:op-222"), eq("SUCCESS"), eq(Duration.ofMinutes(10)));
    }

    @Test
    void handleWithdraw_ShouldSetFailureStatus_OnException() {
        WithdrawMessage message = WithdrawMessage.builder()
                .userId(1L)
                .currency(Currency.USD)
                .amount(BigDecimal.valueOf(50))
                .operationId("op-333")
                .operationType(OperationType.WITHDRAW)
                .build();

        when(accountService.getAccount(1L, Currency.USD)).thenThrow(new RuntimeException("Withdraw error"));

        consumer.handleWithdraw(message);

        verify(valueOperations).set(contains("result:op-333"), contains("FAILURE: Withdraw error"), eq(Duration.ofMinutes(10)));
    }

    @Test
    void handleDlqMessages_ShouldLogErrors() {
        ConsumerRecord<String, Object> record = new ConsumerRecord<>("dlq-topic", 0, 0L, "key", "bad-value");

        consumer.handleDlqMessages(record);

        assertThat(record.topic()).isEqualTo("dlq-topic");
    }
}
