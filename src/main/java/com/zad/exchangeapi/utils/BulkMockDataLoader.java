package com.zad.exchangeapi.utils;

import com.zad.exchangeapi.entity.Account;
import com.zad.exchangeapi.entity.Currency;
import com.zad.exchangeapi.entity.User;
import com.zad.exchangeapi.repository.AccountRepository;
import com.zad.exchangeapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@Slf4j
@RequiredArgsConstructor
public class BulkMockDataLoader implements CommandLineRunner {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;

    @Override
    public void run(String... args) throws Exception {

        log.info("Loading user and user accounts...");
        for (int i = 0; i < 500; i++) {
            long userId = 1000000L + i;

            User user = User.builder()
                    .username("test_user_" + userId)
                    .build();
            userRepository.save(user);

            for (Currency currency : Currency.values()) {
                Account account = Account.builder()
                        .user(user)
                        .currency(Currency.valueOf(currency.name()))
                        .balance(BigDecimal.valueOf(1000))
                        .build();
                accountRepository.save(account);
            }
        }

        log.info("Loaded user and user accounts successfully!");

    }
}
