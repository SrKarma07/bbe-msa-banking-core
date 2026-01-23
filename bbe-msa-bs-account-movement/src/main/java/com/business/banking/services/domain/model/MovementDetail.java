package com.business.banking.services.domain.model;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class MovementDetail {
    private LocalDate date;
    private String type;
    private BigDecimal value;
    private BigDecimal balance;
}
