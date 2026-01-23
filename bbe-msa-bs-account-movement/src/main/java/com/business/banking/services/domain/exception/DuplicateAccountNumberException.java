package com.business.banking.services.domain.exception;

import java.util.Map;

public class DuplicateAccountNumberException extends DomainException {
    public DuplicateAccountNumberException(String number) {
        super(
                DomainErrorCode.DUPLICATE_ACCOUNT_NUMBER,
                "Account number already exists",
                Map.of("number", number)
        );
    }
}
