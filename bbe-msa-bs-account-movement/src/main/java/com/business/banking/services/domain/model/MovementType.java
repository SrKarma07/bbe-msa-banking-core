package com.business.banking.services.domain.model;

import java.util.Arrays;

public enum MovementType {
    DEPOSIT("Deposito"),
    WITHDRAWAL("Retiro");

    private final String apiValue;

    MovementType(String apiValue) {
        this.apiValue = apiValue;
    }

    public String apiValue() {
        return apiValue;
    }

    public static MovementType fromApiValue(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("type is required");
        }
        String normalized = raw.trim();
        return Arrays.stream(values())
                .filter(v -> v.apiValue.equalsIgnoreCase(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("invalid type: " + raw));
    }
}
