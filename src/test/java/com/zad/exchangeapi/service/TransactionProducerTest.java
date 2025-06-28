package com.zad.exchangeapi.service;


import com.zad.exchangeapi.dto.kafka.DepositMessage;
import com.zad.exchangeapi.dto.kafka.WithdrawMessage;
import com.zad.exchangeapi.entity.Currency;
import com.zad.exchangeapi.service.messaging.producer.TransactionProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class TransactionProducerTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private TransactionProducer producer;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void sendDeposit_ShouldSendToDepositTopic() {
        DepositMessage message = new DepositMessage(1L, Currency.USD, new BigDecimal("100"), "op-1");

        producer.sendDeposit(message);

        verify(kafkaTemplate).send("deposit-topic", message);
    }

    @Test
    void sendWithdraw_ShouldSendToWithdrawTopic() {
        WithdrawMessage message = new WithdrawMessage(2L, Currency.USD, new BigDecimal("50"), "op-2");

        producer.sendWithdraw(message);

        verify(kafkaTemplate).send("withdraw-topic", message);
    }

    @Test
    void retryTransaction_ShouldRetryDepositMessage() {
        DepositMessage message = new DepositMessage(3L, Currency.USD, new BigDecimal("200"), "op-3");

        producer.retryTransaction(message, "op-3");

        verify(kafkaTemplate).send("deposit-topic", message);
    }

    @Test
    void retryTransaction_ShouldRetryWithdrawMessage() {
        WithdrawMessage message = new WithdrawMessage(4L, Currency.USD, new BigDecimal("75"), "op-4");

        producer.retryTransaction(message, "op-4");

        verify(kafkaTemplate).send("withdraw-topic", message);
    }

    @Test
    void retryTransaction_ShouldLogErrorForUnknownType() {
        Object unknownMessage = new Object();

        producer.retryTransaction(unknownMessage, "op-unknown");

        verifyNoInteractions(kafkaTemplate); // Should not send anything
    }
}


