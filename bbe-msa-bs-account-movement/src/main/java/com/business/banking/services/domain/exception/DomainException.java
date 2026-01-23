package com.business.banking.services.domain.exception;

import java.util.Collections;
import java.util.Map;

public abstract class DomainException extends RuntimeException {
    private final DomainErrorCode errorCode;
    private final Map<String, Object> attributes;

    protected DomainException(DomainErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.attributes = Collections.emptyMap();
    }

    protected DomainException(DomainErrorCode errorCode, String message, Map<String, Object> attributes) {
        super(message);
        this.errorCode = errorCode;
        this.attributes = (attributes == null) ? Collections.emptyMap() : attributes;
    }

    protected DomainException(DomainErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.attributes = Collections.emptyMap();
    }

    public DomainErrorCode getErrorCode() {
        return errorCode;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }
}
