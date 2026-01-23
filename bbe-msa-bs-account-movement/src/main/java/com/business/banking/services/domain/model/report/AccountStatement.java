package com.business.banking.services.domain.model.report;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder(toBuilder = true)
public class AccountStatement {

    private String customerId;
    private LocalDate from;
    private LocalDate to;
    private List<AccountSummary> accounts;

    @Getter @Setter
    @NoArgsConstructor @AllArgsConstructor
    @Builder(toBuilder = true)
    public static class AccountSummary {
        private String accountNumber;
        private String type;
        private BigDecimal currentBalance;
        private List<MovementDetail> movements;
    }

    @Getter @Setter
    @NoArgsConstructor @AllArgsConstructor
    @Builder(toBuilder = true)
    public static class MovementDetail {
        private LocalDate date;
        private String type;
        private BigDecimal value;
        private BigDecimal balance;
    }
}
