package com.business.banking.services.domain.exception;

import java.util.Map;

public class InvalidRequestException extends DomainException {
    public InvalidRequestException(String message, Map<String, Object> attributes) {
        super(DomainErrorCode.INVALID_REQUEST, message, attributes);
    }

    public static InvalidRequestException generic(String message) {
        return new InvalidRequestException(message, Map.of());
    }
}
