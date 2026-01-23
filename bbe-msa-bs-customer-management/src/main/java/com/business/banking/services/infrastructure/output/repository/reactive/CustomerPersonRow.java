package com.business.banking.services.infrastructure.output.repository.reactive;

import java.util.UUID;

public record CustomerPersonRow(
        UUID customerId,
        String password,
        Boolean state,
        Long personId,
        String name,
        String gender,
        Integer age,
        String identification,
        String address,
        String phoneNumber
) {
}
