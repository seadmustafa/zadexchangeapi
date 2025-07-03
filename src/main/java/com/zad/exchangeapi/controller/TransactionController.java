package com.zad.exchangeapi.controller;

import com.zad.exchangeapi.dto.kafka.DepositMessage;
import com.zad.exchangeapi.dto.kafka.WithdrawMessage;
import com.zad.exchangeapi.dto.request.DepositRequest;
import com.zad.exchangeapi.dto.request.ExchangeRequest;
import com.zad.exchangeapi.dto.request.WithdrawRequest;
import com.zad.exchangeapi.dto.response.BalanceResponse;
import com.zad.exchangeapi.dto.response.GenericResponse;
import com.zad.exchangeapi.dto.response.OperationStatusResponse;
import com.zad.exchangeapi.entity.OperationType;
import com.zad.exchangeapi.service.RedisRateLimiterService;
import com.zad.exchangeapi.service.TransactionService;
import com.zad.exchangeapi.service.messaging.producer.TransactionProducer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@Slf4j
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
@Tag(name = "Transaction API", description = "Endpoints for deposit, withdraw and exchange operations")
public class TransactionController {

    private final RedisTemplate<String, String> redisTemplate;
    private final TransactionService transactionService;
    private final TransactionProducer transactionProducer;
    private final RedisRateLimiterService rateLimiterService;

    @PostMapping("/deposit")
    @Operation(summary = "Deposit money", description = "Deposit a specific amount into a user's account")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Deposit successful"),
            @ApiResponse(responseCode = "400", description = "Validation failed or invalid amount"),
            @ApiResponse(responseCode = "404", description = "User or account not found"),
            @ApiResponse(responseCode = "429", description = "Too many requests"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<GenericResponse> deposit(@Valid @RequestBody DepositRequest request) {
        String userId = String.valueOf(request.userId());
        String operationId = UUID.randomUUID().toString();
        if (rateLimiterService.isAllowed(userId)) {
            DepositMessage message = DepositMessage.builder()
                    .amount(request.amount())
                    .currency(request.currency())
                    .userId(request.userId())
                    .operationId(operationId)
                    .operationType(OperationType.DEPOSIT)
                    .build();
            transactionProducer.sendDeposit(message);

            log.info("Deposit request queued for userId: {}", request.userId());
            return ResponseEntity.ok(transactionService.deposit(request));
        } else {
            log.warn("Rate limit exceeded for deposit - userId: {}", request.userId());
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(new GenericResponse(false, "Too many requests. Please try again later."));
        }
    }

    @PostMapping("/withdraw")
    @Operation(summary = "Withdraw money", description = "Withdraw a specific amount from a user's account")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Withdrawal request accepted"),
            @ApiResponse(responseCode = "400", description = "Insufficient balance or invalid input"),
            @ApiResponse(responseCode = "404", description = "User or account not found"),
            @ApiResponse(responseCode = "429", description = "Too many requests"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<String> withdraw(@Valid @RequestBody WithdrawRequest request) {
        String userId = String.valueOf(request.userId());
        String operationId = UUID.randomUUID().toString();
        if (rateLimiterService.isAllowed(userId)) {
            WithdrawMessage message = WithdrawMessage.builder()
                    .userId(request.userId())
                    .currency(request.currency())
                    .amount(request.amount())
                    .operationId(operationId)
                    .operationType(OperationType.WITHDRAW)
                    .build();
            transactionProducer.sendWithdraw(message);

            transactionService.withdraw(request);
            log.info("Withdraw request accepted for userId: {}", request.userId());
            return ResponseEntity.accepted().body("Withdraw request accepted.");
        } else {
            log.warn("Rate limit exceeded for withdraw - userId: {}", request.userId());
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body("Too many requests. Please try again later.");
        }
    }

    @PostMapping("/exchange")
    @Operation(summary = "Exchange currency", description = "Convert currency from one user/account to another")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Exchange successful"),
            @ApiResponse(responseCode = "400", description = "Invalid input or same currency"),
            @ApiResponse(responseCode = "404", description = "Source or target account not found"),
            @ApiResponse(responseCode = "429", description = "Too many requests"),
            @ApiResponse(responseCode = "500", description = "Exchange rate unavailable or other error")
    })
    public ResponseEntity<GenericResponse> exchange(@Valid @RequestBody ExchangeRequest request) {
        String userId = String.valueOf(request.fromUserId()); // Ensure ExchangeRequest contains sourceUserId()

        if (rateLimiterService.isAllowed(userId)){
            log.info("Exchange request processed for userId: {}", request.fromUserId());
            return ResponseEntity.ok(transactionService.exchange(request));
        } else{
            log.warn("Rate limit exceeded for exchange - userId: {}", request.fromUserId());
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(new GenericResponse(false, "Too many requests. Please try again later."));
        }
    }

    @GetMapping("/balance/{userId}")
    @Operation(summary = "Get user balance", description = "Retrieve user's balance in a specific currency")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Balance retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "User or account not found")
    })
    public BalanceResponse getBalance(@Parameter(description = "User ID") @PathVariable Long userId) {
        return transactionService.getBalance(userId);
    }

    @GetMapping("/status/{userId}")
    @Operation(summary = "Check operation status",
            description = "Retrieve the latest deposit or withdraw status for the given user ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "User or status not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public OperationStatusResponse getOperationStatus(
            @Parameter(description = "User ID to query operation status for") @PathVariable String userId) {

        String status = redisTemplate.opsForValue().get("result:" + userId);
        return new OperationStatusResponse(userId, status != null ? status : "Processing or no record");
    }
}
