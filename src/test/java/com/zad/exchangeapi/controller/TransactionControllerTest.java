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
import com.zad.exchangeapi.service.RedisRateLimiterService;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class TransactionControllerTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private TransactionService transactionService;
    @Mock
    private TransactionProducer transactionProducer;
    @Mock
    private RedisRateLimiterService rateLimiterService;

    @InjectMocks
    private TransactionController controller;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void deposit_ShouldAcceptRequestWhenAllowed() {
        DepositRequest request = new DepositRequest(1L, Currency.USD, BigDecimal.TEN);
        when(rateLimiterService.isAllowed("1")).thenReturn(true);
        when(transactionService.deposit(request)).thenReturn(new GenericResponse(true, "Success"));

        ResponseEntity<GenericResponse> response = controller.deposit(request);

        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        verify(transactionProducer).sendDeposit(any(DepositMessage.class));
        verify(transactionService).deposit(request);
    }

    @Test
    void deposit_ShouldRejectWhenRateLimited() {
        DepositRequest request = new DepositRequest(1L, Currency.USD, BigDecimal.TEN);
        when(rateLimiterService.isAllowed("1")).thenReturn(false);

        ResponseEntity<GenericResponse> response = controller.deposit(request);

        assertThat(response.getStatusCodeValue()).isEqualTo(429);
        verifyNoInteractions(transactionProducer);
        verifyNoInteractions(transactionService);
    }

    @Test
    void withdraw_ShouldAcceptRequestWhenAllowed() {
        WithdrawRequest request = new WithdrawRequest(2L, Currency.USD, BigDecimal.valueOf(20));
        when(rateLimiterService.isAllowed("2")).thenReturn(true);

        ResponseEntity<String> response = controller.withdraw(request);

        assertThat(response.getStatusCodeValue()).isEqualTo(202);
        verify(transactionProducer).sendWithdraw(any(WithdrawMessage.class));
        verify(transactionService).withdraw(request);
    }

    @Test
    void withdraw_ShouldRejectWhenRateLimited() {
        WithdrawRequest request = new WithdrawRequest(2L, Currency.USD, BigDecimal.valueOf(20));
        when(rateLimiterService.isAllowed("2")).thenReturn(false);

        ResponseEntity<String> response = controller.withdraw(request);

        assertThat(response.getStatusCodeValue()).isEqualTo(429);
        verifyNoInteractions(transactionProducer);
    }

    @Test
    void exchange_ShouldAcceptRequestWhenAllowed() {
        ExchangeRequest request = new ExchangeRequest(3L, 4L, Currency.USD, Currency.TRY, BigDecimal.valueOf(100));
        when(rateLimiterService.isAllowed("3")).thenReturn(true);
        when(transactionService.exchange(request)).thenReturn(new GenericResponse(true, "Exchange success"));

        ResponseEntity<GenericResponse> response = controller.exchange(request);

        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        verify(transactionService).exchange(request);
    }

    @Test
    void exchange_ShouldRejectWhenRateLimited() {
        ExchangeRequest request = new ExchangeRequest(3L, 4L, Currency.USD, Currency.TRY, BigDecimal.valueOf(100));
        when(rateLimiterService.isAllowed("3")).thenReturn(false);

        ResponseEntity<GenericResponse> response = controller.exchange(request);

        assertThat(response.getStatusCodeValue()).isEqualTo(429);
        verifyNoInteractions(transactionService);
    }

    @Test
    void getBalance_ShouldReturnBalanceResponse() {
        BalanceResponse mockResponse = new BalanceResponse(1L, Currency.USD, BigDecimal.valueOf(500));
        when(transactionService.getBalance(1L)).thenReturn(mockResponse);

        BalanceResponse response = controller.getBalance(1L);

        assertThat(response).isEqualTo(mockResponse);
        verify(transactionService).getBalance(1L);
    }

    @Test
    void getOperationStatus_ShouldReturnExistingStatus() {
        when(valueOperations.get("result:55")).thenReturn("SUCCESS");

        OperationStatusResponse response = controller.getOperationStatus("55");

        assertThat(response.status()).isEqualTo("SUCCESS");
    }

    @Test
    void getOperationStatus_ShouldReturnDefaultIfNotFound() {
        when(valueOperations.get("result:99")).thenReturn(null);

        OperationStatusResponse response = controller.getOperationStatus("99");

        assertThat(response.status()).isEqualTo("Processing or no record");
    }
}

