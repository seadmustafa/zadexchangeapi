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

    private static final String TRANSACTION_TOPIC = "transaction-queue";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Sends a deposit message to the unified transaction queue.
     */
    public void sendDeposit(DepositMessage message) {
        log.info("Sending deposit message for userId: {} to Kafka", message.getUserId());
        kafkaTemplate.send(TRANSACTION_TOPIC, String.valueOf(message.getUserId()), message);
    }

    /**
     * Sends a withdraw message to the unified transaction queue.
     */
    public void sendWithdraw(WithdrawMessage message) {
        log.info("Sending withdraw message for userId: {} to Kafka", message.getUserId());
        kafkaTemplate.send(TRANSACTION_TOPIC, String.valueOf(message.getUserId()), message);
    }

    /**
     * Retries sending to the unified transaction queue, preserving message type and userId for partitioning.
     */
    public void retryTransaction(Object message, String transactionId) {
        if (message instanceof DepositMessage deposit) {
            log.warn("Retrying deposit message for userId: {} with transactionId: {}", deposit.getUserId(), transactionId);
            kafkaTemplate.send(TRANSACTION_TOPIC, String.valueOf(deposit.getUserId()), deposit);

        } else if (message instanceof WithdrawMessage withdraw) {
            log.warn("Retrying withdraw message for userId: {} with transactionId: {}", withdraw.getUserId(), transactionId);
            kafkaTemplate.send(TRANSACTION_TOPIC, String.valueOf(withdraw.getUserId()), withdraw);

        } else {
            log.error("Unknown message type for retry with transactionId: {}. Message: {}", transactionId, message);
        }
    }
}
