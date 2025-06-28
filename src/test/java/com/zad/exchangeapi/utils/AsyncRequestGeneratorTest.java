package com.zad.exchangeapi.utils;


import com.zad.exchangeapi.dto.kafka.DepositMessage;
import com.zad.exchangeapi.dto.kafka.WithdrawMessage;
import com.zad.exchangeapi.entity.Currency;
import com.zad.exchangeapi.service.messaging.producer.TransactionProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class AsyncRequestGeneratorTest {

    @Mock
    private TransactionProducer transactionProducer;

    @InjectMocks
    private AsyncRequestGenerator asyncRequestGenerator;

    @Captor
    private ArgumentCaptor<DepositMessage> depositCaptor;

    @Captor
    private ArgumentCaptor<WithdrawMessage> withdrawCaptor;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void generateBulkRequests_shouldSendCorrectNumberOfMessages() throws InterruptedException {
        // Arrange
        // We can't control ThreadLocalRandom directly, but for interaction testing, mocking isn't required
        // Reduce iterations for test speed if needed - but keeping 100 as per original code for accuracy

        // Act
        asyncRequestGenerator.generateBulkRequests();

        // Wait briefly to allow all tasks to complete
        Thread.sleep(2000);

        // Assert
        // 50 deposits and 50 withdrawals should be sent
        verify(transactionProducer, times(50)).sendDeposit(depositCaptor.capture());
        verify(transactionProducer, times(50)).sendWithdraw(withdrawCaptor.capture());

        // Validate content of some captured messages
        depositCaptor.getAllValues().forEach(deposit -> {
            assertThat(deposit.getCurrency()).isEqualTo(Currency.USD);
            assertThat(deposit.getAmount()).isNotNull();
            assertThat(deposit.getUserId()).isBetween(0L, 499L);
        });

        withdrawCaptor.getAllValues().forEach(withdraw -> {
            assertThat(withdraw.getCurrency()).isEqualTo(Currency.USD);
            assertThat(withdraw.getAmount()).isNotNull();
            assertThat(withdraw.getUserId()).isBetween(0L, 499L);
        });
    }
}
