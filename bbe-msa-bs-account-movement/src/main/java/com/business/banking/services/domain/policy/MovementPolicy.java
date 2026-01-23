package com.business.banking.services.domain.policy;

import com.business.banking.services.domain.exception.InvalidRequestException;
import com.business.banking.services.domain.model.Movement;
import com.business.banking.services.domain.model.MovementType;

import java.math.BigDecimal;
import java.util.Map;

public final class MovementPolicy {

    private MovementPolicy() {
    }

    public static Movement normalizeForPersistence(Movement m) {
        if (m == null) {
            throw new InvalidRequestException("movement is required", Map.of());
        }
        if (m.getAccountNumber() == null || m.getAccountNumber().isBlank()) {
            throw new InvalidRequestException("accountNumber is required", Map.of("field", "accountNumber"));
        }
        if (m.getDate() == null) {
            throw new InvalidRequestException("date is required", Map.of("field", "date"));
        }
        if (m.getType() == null || m.getType().isBlank()) {
            throw new InvalidRequestException("type is required", Map.of("field", "type"));
        }
        if (m.getValue() == null) {
            throw new InvalidRequestException("value is required", Map.of("field", "value"));
        }
        if (m.getValue().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidRequestException("value must be greater than zero", Map.of("field", "value"));
        }

        final MovementType type;
        try {
            type = MovementType.fromApiValue(m.getType());
        } catch (IllegalArgumentException ex) {
            throw new InvalidRequestException(ex.getMessage(), Map.of("field", "type", "value", m.getType()));
        }

        BigDecimal normalized = m.getValue().abs();

        return m.toBuilder()
                .type(type.apiValue())
                .value(normalized)
                .build();
        }
}
