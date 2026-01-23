package com.business.banking.services.domain.model;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class AccountSummary {
    private String accountNumber;
    private String type;
    private Boolean state;
    private BigDecimal balance;
    private List<MovementDetail> movements;
}
