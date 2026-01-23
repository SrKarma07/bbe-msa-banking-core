package com.business.banking.services.domain.exception;

import java.util.Map;

public class NotFoundException extends DomainException {
    public NotFoundException(String resource, String criteria) {
        super(
                DomainErrorCode.ACCOUNT_NOT_FOUND, // por defecto; se puede especializar abajo
                resource + " not found (" + criteria + ")",
                Map.of("resource", resource, "criteria", criteria)
        );
    }

    public static NotFoundException accountByNumber(String number) {
        return new NotFoundException(DomainErrorCode.ACCOUNT_NOT_FOUND, "Account not found (number=" + number + ")", Map.of("number", number));
    }

    public static NotFoundException movementById(String id) {
        return new NotFoundException(DomainErrorCode.MOVEMENT_NOT_FOUND, "Movement not found (id=" + id + ")", Map.of("id", id));
    }

    public static NotFoundException customerById(String customerId) {
        return new NotFoundException(DomainErrorCode.CUSTOMER_NOT_FOUND, "Customer not found (id=" + customerId + ")", Map.of("customerId", customerId));
    }

    private NotFoundException(DomainErrorCode code, String message, Map<String, Object> attributes) {
        super(code, message, attributes);
    }
}
