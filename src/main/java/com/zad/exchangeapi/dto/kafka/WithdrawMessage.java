package com.zad.exchangeapi.dto.kafka;

import com.zad.exchangeapi.entity.Currency;
import lombok.*;

import java.math.BigDecimal;

/**
 * Message sent to Kafka for withdrawal operations
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WithdrawMessage {

    private Long userId;
    private Currency currency;
    private BigDecimal amount;
    private String operationId;
}
