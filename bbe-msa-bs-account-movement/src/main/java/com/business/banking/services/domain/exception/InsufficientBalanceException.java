package com.business.banking.services.domain.exception;

import java.math.BigDecimal;
import java.util.Map;

public class InsufficientBalanceException extends DomainException {
    public InsufficientBalanceException(String accountNumber, BigDecimal currentBalance, BigDecimal requestedChange) {
        super(
                DomainErrorCode.INSUFFICIENT_BALANCE,
                "Insufficient balance",
                Map.of(
                        "accountNumber", accountNumber,
                        "currentBalance", currentBalance,
                        "requestedChange", requestedChange
                )
        );
    }
}
