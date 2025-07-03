package com.zad.exchangeapi.service;

import com.zad.exchangeapi.dto.kafka.DepositMessage;
import com.zad.exchangeapi.dto.kafka.WithdrawMessage;
import com.zad.exchangeapi.entity.Currency;
import com.zad.exchangeapi.entity.OperationType;
import com.zad.exchangeapi.service.messaging.producer.TransactionProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;

import static org.mockito.Mockito.*;

class TransactionProducerTest {

    private static final String TRANSACTION_TOPIC = "transaction-queue";

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private TransactionProducer producer;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void sendDeposit_ShouldSendMessageToTransactionQueue() {
        DepositMessage message = DepositMessage.builder()
                .userId(1L)
                .currency(Currency.USD)
                .amount(BigDecimal.valueOf(100))
                .operationId("op-1")
                .operationType(OperationType.DEPOSIT)
                .build();

        producer.sendDeposit(message);

        verify(kafkaTemplate, times(1)).send(TRANSACTION_TOPIC, String.valueOf(message.getUserId()), message);
    }

    @Test
    void sendWithdraw_ShouldSendMessageToTransactionQueue() {
        WithdrawMessage message = WithdrawMessage.builder()
                .userId(2L)
                .currency(Currency.USD)
                .amount(BigDecimal.valueOf(50))
                .operationId("op-2")
                .operationType(OperationType.WITHDRAW)
                .build();

        producer.sendWithdraw(message);

        verify(kafkaTemplate, times(1)).send(TRANSACTION_TOPIC, String.valueOf(message.getUserId()), message);
    }

    @Test
    void retryTransaction_ShouldRetryDepositMessage() {
        DepositMessage message = DepositMessage.builder()
                .userId(3L)
                .currency(Currency.USD)
                .amount(BigDecimal.valueOf(200))
                .operationId("op-3")
                .operationType(OperationType.DEPOSIT)
                .build();

        producer.retryTransaction(message, message.getOperationId());

        verify(kafkaTemplate, times(1)).send(TRANSACTION_TOPIC, String.valueOf(message.getUserId()), message);
    }

    @Test
    void retryTransaction_ShouldRetryWithdrawMessage() {
        WithdrawMessage message = WithdrawMessage.builder()
                .userId(4L)
                .currency(Currency.USD)
                .amount(BigDecimal.valueOf(75))
                .operationId("op-4")
                .operationType(OperationType.WITHDRAW)
                .build();

        producer.retryTransaction(message, message.getOperationId());

        verify(kafkaTemplate, times(1)).send(TRANSACTION_TOPIC, String.valueOf(message.getUserId()), message);
    }

    @Test
    void retryTransaction_ShouldLogErrorForUnknownMessageType() {
        Object unknownMessage = new Object();

        producer.retryTransaction(unknownMessage, "op-unknown");

        verifyNoInteractions(kafkaTemplate);
    }

}
