package com.business.banking.services.domain.exception;

import java.util.Map;

public class AccountClosedException extends DomainException {
    public AccountClosedException(String accountNumber) {
        super(
                DomainErrorCode.ACCOUNT_CLOSED,
                "Account is closed",
                Map.of("accountNumber", accountNumber)
        );
    }
}
