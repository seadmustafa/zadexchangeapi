package com.zad.exchangeapi.service;

import com.zad.exchangeapi.dto.request.DepositRequest;
import com.zad.exchangeapi.dto.request.ExchangeRequest;
import com.zad.exchangeapi.dto.request.WithdrawRequest;
import com.zad.exchangeapi.dto.response.BalanceResponse;
import com.zad.exchangeapi.dto.response.GenericResponse;

public interface TransactionService {
    GenericResponse deposit(DepositRequest request);

    GenericResponse withdraw(WithdrawRequest request);

    GenericResponse exchange(ExchangeRequest request);

    BalanceResponse getBalance(Long userId);
}
