package com.zad.exchangeapi.utils;

import com.zad.exchangeapi.entity.Account;
import com.zad.exchangeapi.entity.Currency;
import com.zad.exchangeapi.entity.User;
import com.zad.exchangeapi.repository.AccountRepository;
import com.zad.exchangeapi.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class BulkMockDataLoaderTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private BulkMockDataLoader bulkMockDataLoader;

    @Captor
    private ArgumentCaptor<User> userCaptor;

    @Captor
    private ArgumentCaptor<Account> accountCaptor;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void run_shouldCreateUsersAndAccountsCorrectly() throws Exception {
        // Arrange
        int userCount = 500;
        int currencyCount = Currency.values().length;

        // Mock userRepository to simulate assigning IDs if necessary
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        bulkMockDataLoader.run();

        // Assert - verify user saves
        verify(userRepository, times(userCount)).save(userCaptor.capture());
        List<User> savedUsers = userCaptor.getAllValues();

        assertThat(savedUsers).hasSize(userCount);
        assertThat(savedUsers.get(0).getUsername()).startsWith("test_user_");

        // Assert - verify account saves
        verify(accountRepository, times(userCount * currencyCount)).save(accountCaptor.capture());
        List<Account> savedAccounts = accountCaptor.getAllValues();

        assertThat(savedAccounts).hasSize(userCount * currencyCount);

        // Spot check first account
        Account sampleAccount = savedAccounts.get(0);
        assertThat(sampleAccount.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(1000));
        assertThat(sampleAccount.getCurrency()).isNotNull();
        assertThat(sampleAccount.getUser()).isNotNull();
    }
}

