package com.zad.exchangeapi.repository;

import com.zad.exchangeapi.entity.Account;
import com.zad.exchangeapi.entity.Currency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByUserIdAndCurrency(Long userId, Currency currency);
}
