package com.business.banking.services.domain.model;

import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class AccountStatement {
    private String customerId;
    private LocalDate dateFrom;
    private LocalDate dateTo;
    private List<AccountSummary> accounts;
}
