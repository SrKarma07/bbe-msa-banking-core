package com.business.banking.services.domain.model;

import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class Account {
    private String number;
    private String type;

    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;
    private Boolean state;
    private UUID customerId;
}