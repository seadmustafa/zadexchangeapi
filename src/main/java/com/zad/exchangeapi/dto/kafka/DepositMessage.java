package com.zad.exchangeapi.dto.kafka;

import com.zad.exchangeapi.entity.Currency;
import lombok.*;

import java.math.BigDecimal;

/**
 * Message sent to Kafka for deposit operations
 */


@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepositMessage {

    private Long userId;
    private Currency currency;
    private BigDecimal amount;
    private String operationId;
}

