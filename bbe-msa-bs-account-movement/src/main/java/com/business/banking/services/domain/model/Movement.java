package com.business.banking.services.domain.model;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class Movement {
    private UUID id;
    private String accountNumber;
    private LocalDate date;
    private String type;
    private BigDecimal value;
    private BigDecimal balance;
    private String detail;
    private String idempotencyKey;
}
