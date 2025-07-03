package com.zad.exchangeapi.service.messaging.consumer;


import com.zad.exchangeapi.dto.kafka.DepositMessage;
import com.zad.exchangeapi.dto.kafka.WithdrawMessage;
import com.zad.exchangeapi.entity.Account;
import com.zad.exchangeapi.service.AccountService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionConsumer {

    private final AccountService accountService;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String REDIS_PREFIX = "result:";
    private static final String TOPIC = "transaction-queue";
    private static final int CACHE_TTL_MINUTES = 10;

    @KafkaListener(topics = TOPIC, groupId = "transaction-group")
    @Transactional
    public void consumeTransaction(ConsumerRecord<String, Object> record) {
        try {
            Object message = record.value();
            log.info("Received message from {}: {}", TOPIC, message);

            if (message instanceof DepositMessage deposit) {
                handleDeposit(deposit);
            } else if (message instanceof WithdrawMessage withdraw) {
                handleWithdraw(withdraw);
            } else {
                log.warn("Unknown message type received: {}", message.getClass().getSimpleName());
            }

        } catch (Exception ex) {
            log.error("Error processing transaction message: {}", record, ex);
        }
    }

    public void handleDeposit(DepositMessage message) {
        String redisKey = REDIS_PREFIX + message.getOperationId();
        try {
            log.info("Processing deposit: {}", message);

            Account account = accountService.getAccount(message.getUserId(), message.getCurrency());
            BigDecimal updated = account.getBalance().add(message.getAmount());
            accountService.updateBalance(account, updated);

            redisTemplate.opsForValue().set(redisKey, "SUCCESS", Duration.ofMinutes(CACHE_TTL_MINUTES));
            log.info("Deposit successful for user {}", message.getUserId());

        } catch (Exception ex) {
            log.error("Deposit failed for user {}", message.getUserId(), ex);
            redisTemplate.opsForValue().set(redisKey, "FAILURE: " + ex.getMessage(), Duration.ofMinutes(CACHE_TTL_MINUTES));
        }
    }

    public void handleWithdraw(WithdrawMessage message) {
        String redisKey = REDIS_PREFIX + message.getOperationId();
        try {
            log.info("Processing withdraw: {}", message);

            Account account = accountService.getAccount(message.getUserId(), message.getCurrency());
            accountService.validateSufficientBalance(account, message.getAmount());
            BigDecimal updated = account.getBalance().subtract(message.getAmount());
            accountService.updateBalance(account, updated);

            redisTemplate.opsForValue().set(redisKey, "SUCCESS", Duration.ofMinutes(CACHE_TTL_MINUTES));
            log.info("Withdraw successful for user {}", message.getUserId());

        } catch (Exception ex) {
            log.error("Withdraw failed for user {}", message.getUserId(), ex);
            redisTemplate.opsForValue().set(redisKey, "FAILURE: " + ex.getMessage(), Duration.ofMinutes(CACHE_TTL_MINUTES));
        }
    }

    @KafkaListener(topics = "dlq-topic")
    public void handleDlqMessages(ConsumerRecord<String, Object> record) {
        log.error("DLQ message detected: {}", record);
    }
}
