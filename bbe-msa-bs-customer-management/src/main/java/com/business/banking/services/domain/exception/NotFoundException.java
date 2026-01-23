package com.business.banking.services.domain.exception;

import java.util.Map;

public final class NotFoundException extends DomainException {

    private NotFoundException(DomainErrorCode code, String message, Map<String, Object> attrs) {
        super(code, message, attrs);
    }

    public static NotFoundException customerById(String id) {
        return new NotFoundException(
                DomainErrorCode.CUSTOMER_NOT_FOUND,
                "Customer not found (id=" + id + ")",
                Map.of("id", id)
        );
    }
}
