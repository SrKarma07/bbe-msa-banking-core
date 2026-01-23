package com.business.banking.services.domain.exception;

import java.util.Map;

public final class InvalidRequestException extends DomainException {
    public InvalidRequestException(String message) {
        super(DomainErrorCode.INVALID_REQUEST, message);
    }

    public InvalidRequestException(String message, Map<String, Object> attributes) {
        super(DomainErrorCode.INVALID_REQUEST, message, attributes);
    }

    public static InvalidRequestException fieldMissing(String fieldName) {
        return new InvalidRequestException("Missing required field", Map.of("field", fieldName));
    }
}
