package com.zad.exchangeapi.service.messaging.producer;

import com.zad.exchangeapi.dto.kafka.DepositMessage;
import com.zad.exchangeapi.dto.kafka.WithdrawMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransactionProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendDeposit(DepositMessage message) {
        kafkaTemplate.send("deposit-topic", message);
    }

    public void sendWithdraw(WithdrawMessage message) {
        kafkaTemplate.send("withdraw-topic", message);
    }


    /**
     * Retry sending to the same topic based on message type.
     */
    public void retryTransaction(Object message, String transactionId) {
        if (message instanceof DepositMessage deposit) {
            log.warn("Retrying deposit message: {} with transactionId: {}", deposit, transactionId);
            kafkaTemplate.send("deposit-topic", deposit);
        } else if (message instanceof WithdrawMessage withdraw) {
            log.warn("Retrying withdraw message: {} with transactionId: {}", withdraw, transactionId);
            kafkaTemplate.send("withdraw-topic", withdraw);
        } else {
            log.error("Unknown message type for retry: {} with transactionId: {}", message, transactionId);
        }
    }
}
