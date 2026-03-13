package com.business.banking.services.domain.model;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class Transfer {
    private String sourceAccountNumber;
    private String destinationAccountNumber;
    private LocalDate date;
    private BigDecimal value;
    private String detail;
}
