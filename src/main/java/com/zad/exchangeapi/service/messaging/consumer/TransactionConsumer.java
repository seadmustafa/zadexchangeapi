package com.zad.exchangeapi.service.messaging.consumer;

import com.zad.exchangeapi.dto.kafka.DepositMessage;
import com.zad.exchangeapi.dto.kafka.WithdrawMessage;
import com.zad.exchangeapi.entity.Account;
import com.zad.exchangeapi.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class TransactionConsumer {

    private static final Logger logger = LoggerFactory.getLogger(TransactionConsumer.class);
    private final AccountService accountService;
    private final StringRedisTemplate redisTemplate;

    @KafkaListener(topics = "deposit-topic", groupId = "balance-group")
    @Transactional
    public void handleDeposit(DepositMessage message) {
        String redisKey = "result:" + message.getOperationId();
        try {
            logger.info("Consuming deposit message: {}", message);
            Account account = accountService.getAccount(message.getUserId(), message.getCurrency());
            BigDecimal updated = account.getBalance().add(message.getAmount());
            accountService.updateBalance(account, updated);

            // ✅ Success status with 10 min expiry
            redisTemplate.opsForValue().set(redisKey, "SUCCESS", Duration.ofMinutes(10));

        } catch (Exception ex) {
            logger.error("Deposit failed", ex);
            // ✅ Failure status with reason, 10 min expiry
            redisTemplate.opsForValue().set(redisKey, "FAILURE: " + ex.getMessage(), Duration.ofMinutes(10));
        }
    }

    @KafkaListener(topics = "withdraw-topic", groupId = "balance-group")
    @Transactional
    public void handleWithdraw(WithdrawMessage message) {
        String redisKey = "result:" + message.getOperationId();
        try {
            logger.info("Consuming withdraw message: {}", message);
            Account account = accountService.getAccount(message.getUserId(), message.getCurrency());
            accountService.validateSufficientBalance(account, message.getAmount());
            BigDecimal updated = account.getBalance().subtract(message.getAmount());
            accountService.updateBalance(account, updated);

            redisTemplate.opsForValue().set(redisKey, "SUCCESS", Duration.ofMinutes(10));

        } catch (Exception ex) {
            logger.error("Withdraw failed", ex);
            redisTemplate.opsForValue().set(redisKey, "FAILURE: " + ex.getMessage(), Duration.ofMinutes(10));
        }
    }

    @KafkaListener(topics = "dlq-topic")
    public void handleDlqMessages(ConsumerRecord<String, Object> record) {
        logger.error("DLQ message detected: {}", record);
    }

}
