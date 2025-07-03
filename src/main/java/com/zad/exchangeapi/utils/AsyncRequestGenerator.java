package com.zad.exchangeapi.utils;

import com.zad.exchangeapi.dto.kafka.DepositMessage;
import com.zad.exchangeapi.dto.kafka.WithdrawMessage;
import com.zad.exchangeapi.entity.Currency;
import com.zad.exchangeapi.entity.OperationType;
import com.zad.exchangeapi.service.messaging.producer.TransactionProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;
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
        log.info("Starting bulk request generation...");

        ExecutorService executor = Executors.newFixedThreadPool(20);

        for (int i = 0; i < 100000; i++) {

            int userId = i % 500;
            BigDecimal amount = BigDecimal.valueOf(ThreadLocalRandom.current().nextDouble(1, 500))
                    .setScale(2, RoundingMode.HALF_UP);

            Currency currency = ThreadLocalRandom.current().nextBoolean() ? Currency.USD : Currency.TRY;
            OperationType operationType = (i % 3 == 0) ? OperationType.DEPOSIT : OperationType.WITHDRAW;

            Runnable task = () -> {
                String operationId = UUID.randomUUID().toString(); // Unique operation ID per transaction

                if (operationType == OperationType.DEPOSIT) {
                    DepositMessage deposit = DepositMessage.builder()
                            .userId((long) userId)
                            .currency(currency)
                            .amount(amount)
                            .operationId(operationId)
                            .operationType(OperationType.DEPOSIT)
                            .build();
                    transactionProducer.sendDeposit(deposit);
                    log.debug("Generated deposit: {}", deposit);

                } else {
                    WithdrawMessage withdraw = WithdrawMessage.builder()
                            .userId((long) userId)
                            .currency(currency)
                            .amount(amount)
                            .operationId(operationId)
                            .operationType(OperationType.WITHDRAW)
                            .build();
                    transactionProducer.sendWithdraw(withdraw);
                    log.debug("Generated withdraw: {}", withdraw);
                }
            };

            executor.submit(task);
        }

        executor.shutdown();
        log.info("Bulk request generation submitted for processing.");
    }
}
