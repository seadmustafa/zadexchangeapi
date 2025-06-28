package com.zad.exchangeapi.service;


import com.zad.exchangeapi.entity.Account;
import com.zad.exchangeapi.entity.Currency;
import com.zad.exchangeapi.entity.User;
import com.zad.exchangeapi.repository.AccountRepository;
import com.zad.exchangeapi.repository.UserRepository;
import com.zad.exchangeapi.service.impl.AccountServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@DataJpaTest
@Import(AccountServiceImpl.class)
class AccountServiceImplIntegrationTest {

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void getAccount_FetchesFromDatabase() {
        User user = new User();
        user.setUsername("username");
        user = userRepository.save(user);

        Account account = Account.builder()
                .user(user)
                .currency(Currency.USD)
                .balance(BigDecimal.valueOf(1000))
                .build();

        accountRepository.save(account);

        Account fetched = accountService.getAccount(user.getId(), Currency.USD);

        assertThat(fetched).isNotNull();
        assertThat(fetched.getBalance()).isEqualTo(BigDecimal.valueOf(1000));
    }

    @Test
    void updateBalance_IntegrationTest() {
        User user = new User();
        user.setUsername("username1");
        user = userRepository.save(user);

        Account account = Account.builder()
                .user(user)
                .currency(Currency.TRY)
                .balance(BigDecimal.valueOf(500))
                .build();

        accountRepository.save(account);

        accountService.updateBalance(account, BigDecimal.valueOf(300));

        Account updated = accountRepository.findByUserIdAndCurrency(user.getId(), Currency.TRY).orElseThrow();

        assertThat(updated.getBalance()).isEqualTo(BigDecimal.valueOf(300));
    }

    @Test
    void getAccount_WhenNotExists_ShouldThrow() {
        assertThatThrownBy(() -> accountService.getAccount(999L, Currency.USD))
                .isInstanceOf(com.example.balanceapi.exception.NotFoundExceptionn.class)
                .hasMessageContaining("Account not found");
    }

    @Test
    void updateBalance_WhenNegative_ShouldThrow() {
        User user = new User();
        user.setUsername("username11");
        userRepository.save(user);
        Account account = accountRepository.save(new Account(null, Currency.USD, user, BigDecimal.valueOf(100)));

        assertThatThrownBy(() -> accountService.updateBalance(account, BigDecimal.valueOf(-50)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Balance cannot be negative");
    }

}

