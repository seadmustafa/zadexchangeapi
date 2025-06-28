package com.zad.exchangeapi.utils;

import com.zad.exchangeapi.dto.kafka.DepositMessage;
import com.zad.exchangeapi.dto.kafka.WithdrawMessage;
import com.zad.exchangeapi.entity.Currency;
import com.zad.exchangeapi.service.messaging.producer.TransactionProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Component
@RequiredArgsConstructor
public class AsyncRequestGenerator {

    private final TransactionProducer transactionProducer;

    @Scheduled(fixedRate = 120_000)
    public void generateBulkRequests() {
        log.info("Generate bulk requests");
        ExecutorService executor = Executors.newFixedThreadPool(20); // Adjust threads based on system

        for (int i = 0; i < 100; i++) {
            int userId = i % 500; // Reuse existing 500 users
            BigDecimal amount = BigDecimal.valueOf(ThreadLocalRandom.current().nextDouble(1, 500)).setScale(2, RoundingMode.HALF_UP);

            int finalI = i;
            Runnable task = () -> {
                if (finalI % 2 == 0) {
                    DepositMessage deposit = DepositMessage.builder()
                            .userId((long) userId)
                            .currency(Currency.USD)
                            .amount(amount)
                            .build();
                    transactionProducer.sendDeposit(deposit);
                } else {
                    WithdrawMessage withdraw = WithdrawMessage.builder()
                            .userId((long) userId)
                            .currency(Currency.USD)
                            .amount(amount)
                            .build();
                    transactionProducer.sendWithdraw(withdraw);
                }
            };

            executor.submit(task);
        }
        log.info("Bulk requests have been generated!");
        executor.shutdown();
    }
}
