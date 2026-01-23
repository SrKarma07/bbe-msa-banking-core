package com.business.banking.services.domain.exception;

import java.util.Map;

public final class DuplicateIdentificationException extends DomainException {
    public DuplicateIdentificationException(String identification) {
        super(
                DomainErrorCode.DUPLICATE_IDENTIFICATION,
                "Identification already registered",
                Map.of("identification", identification)
        );
    }
}
