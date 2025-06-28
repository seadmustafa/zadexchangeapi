package com.zad.exchangeapi.dto.kafka;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperationResult {

    private String userId;
    private String operationType;  // "DEPOSIT" or "WITHDRAW"
    private String status;         // "SUCCESS" or "FAILURE"
    private String currency;
}
