package com.zad.exchangeapi.service;


import com.example.balanceapi.exception.NotFoundExceptionn;
import com.zad.exchangeapi.entity.Account;
import com.zad.exchangeapi.entity.Currency;
import com.zad.exchangeapi.entity.User;
import com.zad.exchangeapi.repository.AccountRepository;
import com.zad.exchangeapi.service.impl.AccountServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class AccountServiceImplTest {

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private AccountServiceImpl accountService;

    private Account account;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        User user = new User();
        user.setId(100L);
        user.setUsername("username");
        account = Account.builder()
                .id(1L)
                .user(user)
                .currency(Currency.USD)
                .balance(BigDecimal.valueOf(500))
                .build();
    }

    @Test
    void getAccount_WhenFound_ReturnsAccount() {
        when(accountRepository.findByUserIdAndCurrency(100L, Currency.USD))
                .thenReturn(Optional.of(account));

        Account result = accountService.getAccount(100L, Currency.USD);

        assertThat(result).isNotNull();
        assertThat(result.getUser().getId()).isEqualTo(100L);
    }

    @Test
    void getAccount_WhenNotFound_ThrowsNotFoundException() {
        when(accountRepository.findByUserIdAndCurrency(100L, Currency.USD))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.getAccount(100L, Currency.USD))
                .isInstanceOf(NotFoundExceptionn.class)
                .hasMessageContaining("Account not found for user 100");
    }

    @Test
    void updateBalance_WithPositiveAmount_UpdatesSuccessfully() {
        BigDecimal newBalance = BigDecimal.valueOf(300);
        accountService.updateBalance(account, newBalance);

        assertThat(account.getBalance()).isEqualTo(newBalance);
        verify(accountRepository).save(account);
    }

    @Test
    void updateBalance_WithNegativeAmount_ThrowsException() {
        BigDecimal newBalance = BigDecimal.valueOf(-50);

        assertThatThrownBy(() -> accountService.updateBalance(account, newBalance))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Balance cannot be negative");

        verify(accountRepository, never()).save(any());
    }

    @Test
    void validateSufficientBalance_WithEnoughBalance_DoesNothing() {
        accountService.validateSufficientBalance(account, BigDecimal.valueOf(100));
    }

    @Test
    void validateSufficientBalance_WithInsufficientBalance_ThrowsException() {
        assertThatThrownBy(() -> accountService.validateSufficientBalance(account, BigDecimal.valueOf(1000)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Insufficient balance in account");
    }
}
