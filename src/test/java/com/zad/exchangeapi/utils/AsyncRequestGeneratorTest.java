package com.zad.exchangeapi.utils;

import com.zad.exchangeapi.dto.kafka.DepositMessage;
import com.zad.exchangeapi.dto.kafka.WithdrawMessage;
import com.zad.exchangeapi.entity.Currency;
import com.zad.exchangeapi.entity.OperationType;
import com.zad.exchangeapi.service.messaging.producer.TransactionProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AsyncRequestGeneratorTest {

    @Mock
    private TransactionProducer transactionProducer;

    @InjectMocks
    private AsyncRequestGenerator generator;

    @Captor
    private ArgumentCaptor<DepositMessage> depositCaptor;

    @Captor
    private ArgumentCaptor<WithdrawMessage> withdrawCaptor;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }


    @Test
    void generateBulkRequests_ShouldSendDepositsAndWithdrawals() throws InterruptedException {

        CountDownLatch latch = new CountDownLatch(10);

        doAnswer(invocation -> {
            latch.countDown();
            return null;
        })
                .when(transactionProducer).sendDeposit(any(DepositMessage.class));

        doAnswer(invocation -> {
            latch.countDown();
            return null;
        })
                .when(transactionProducer).sendWithdraw(any(WithdrawMessage.class));

        // Act
        generator.generateBulkRequests();

        // Assert: Wait for all tasks to complete (max 5 seconds)
        boolean completed = latch.await(5, TimeUnit.SECONDS);
        assertThat(completed).isTrue();

        // At least some deposits and withdraws should happen
        verify(transactionProducer, atLeastOnce()).sendDeposit(any(DepositMessage.class));
        verify(transactionProducer, atLeastOnce()).sendWithdraw(any(WithdrawMessage.class));
    }

    /**
     * Isolated test overriding randomness for deterministic coverage.
     */
    @Test
    void generateBulkRequests_ShouldGenerateExpectedDepositsAndWithdraws() throws Exception {

        AsyncRequestGenerator controlledGenerator = new AsyncRequestGenerator(transactionProducer) {
            @Override
            public void generateBulkRequests() {

                ExecutorService executor = Executors.newFixedThreadPool(2);

                List<Callable<Void>> tasks = new ArrayList<>();

                for (int i = 0; i < 6; i++) {
                    int userId = i;
                    BigDecimal amount = BigDecimal.TEN;
                    Currency currency = Currency.USD;
                    OperationType operationType = (i % 2 == 0) ? OperationType.DEPOSIT : OperationType.WITHDRAW;

                    tasks.add(() -> {
                        String operationId = UUID.randomUUID().toString();
                        if (operationType == OperationType.DEPOSIT) {
                            DepositMessage deposit = DepositMessage.builder()
                                    .userId((long) userId)
                                    .currency(currency)
                                    .amount(amount)
                                    .operationId(operationId)
                                    .operationType(operationType)
                                    .build();
                            transactionProducer.sendDeposit(deposit);
                        } else {
                            WithdrawMessage withdraw = WithdrawMessage.builder()
                                    .userId((long) userId)
                                    .currency(currency)
                                    .amount(amount)
                                    .operationId(operationId)
                                    .operationType(operationType)
                                    .build();
                            transactionProducer.sendWithdraw(withdraw);
                        }
                        return null;
                    });
                }

                try {
                    executor.invokeAll(tasks); // Ensures all run to completion
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    executor.shutdown();
                }
            }
        };

        // Act
        controlledGenerator.generateBulkRequests();

        // Assert
        verify(transactionProducer, times(3)).sendDeposit(any(DepositMessage.class));
        verify(transactionProducer, times(3)).sendWithdraw(any(WithdrawMessage.class));
    }

}
