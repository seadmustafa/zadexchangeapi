package com.zad.exchangeapi.controller;

import com.zad.exchangeapi.dto.kafka.DepositMessage;
import com.zad.exchangeapi.dto.kafka.WithdrawMessage;
import com.zad.exchangeapi.dto.request.DepositRequest;
import com.zad.exchangeapi.dto.request.ExchangeRequest;
import com.zad.exchangeapi.dto.request.WithdrawRequest;
import com.zad.exchangeapi.dto.response.BalanceResponse;
import com.zad.exchangeapi.dto.response.GenericResponse;
import com.zad.exchangeapi.dto.response.OperationStatusResponse;
import com.zad.exchangeapi.entity.Currency;
import com.zad.exchangeapi.service.TransactionService;
import com.zad.exchangeapi.service.messaging.producer.TransactionProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class TransactionControllerTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private TransactionService transactionService;

    @Mock
    private TransactionProducer transactionProducer;

    @InjectMocks
    private TransactionController controller;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void deposit_ShouldSendMessageAndReturnResponse() {
        DepositRequest request = new DepositRequest(1L, Currency.USD, new BigDecimal("100"));
        GenericResponse expected = new GenericResponse(true, "Deposit successful");

        when(transactionService.deposit(request)).thenReturn(expected);

        GenericResponse response = controller.deposit(request);

        assertEquals(expected, response);
        verify(transactionProducer).sendDeposit(any(DepositMessage.class));
    }

    @Test
    void withdraw_ShouldSendMessageAndReturnAccepted() {
        WithdrawRequest request = new WithdrawRequest(2L, Currency.USD, new BigDecimal("50"));

        ResponseEntity<String> response = controller.withdraw(request);

        assertEquals(202, response.getStatusCodeValue());
        verify(transactionProducer).sendWithdraw(any(WithdrawMessage.class));
        verify(transactionService).withdraw(request);
    }

    @Test
    void exchange_ShouldCallService() {
        ExchangeRequest request = new ExchangeRequest(1L, 2L, Currency.USD, Currency.TRY, new BigDecimal("200"));
        GenericResponse expected = new GenericResponse(true, "Exchange completed");

        when(transactionService.exchange(request)).thenReturn(expected);

        GenericResponse response = controller.exchange(request);

        assertEquals(expected, response);
    }

    @Test
    void getBalance_ShouldReturnBalance() {
        BalanceResponse expected = new BalanceResponse(1L, Currency.USD, new BigDecimal("300"));

        when(transactionService.getBalance(1L)).thenReturn(expected);

        BalanceResponse response = controller.getBalance(1L);

        assertEquals(expected, response);
    }

    @Test
    void getOperationStatus_ReturnsCachedStatus() {
        when(valueOperations.get("result:1")).thenReturn("SUCCESS");

        OperationStatusResponse response = controller.getOperationStatus("1");

        assertEquals("SUCCESS", response.status());
    }

    @Test
    void getOperationStatus_ReturnsDefaultMessageIfNull() {
        when(valueOperations.get("result:2")).thenReturn(null);

        OperationStatusResponse response = controller.getOperationStatus("2");

        assertEquals("Processing or no record", response.status());
    }
}
