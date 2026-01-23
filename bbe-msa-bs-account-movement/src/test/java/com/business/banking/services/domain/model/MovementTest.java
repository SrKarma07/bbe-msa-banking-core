package com.business.banking.services.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class MovementTest {

    @Test
    void noArgsConstructor_shouldCreateEmptyMovement() {
        Movement m = new Movement();

        assertNull(m.getId());
        assertNull(m.getAccountNumber());
        assertNull(m.getDate());
        assertNull(m.getType());
        assertNull(m.getValue());
        assertNull(m.getBalance());
        assertNull(m.getDetail());
        assertNull(m.getIdempotencyKey());
    }

    @Test
    void allArgsConstructor_shouldSetAllFields() {
        UUID id = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        LocalDate date = LocalDate.of(2025, 2, 10);

        Movement m = new Movement(
                id,
                "478758",
                date,
                "Deposito",
                new BigDecimal("600.00"),
                new BigDecimal("2600.00"),
                "Cash deposit",
                "idem-001"
        );

        assertEquals(id, m.getId());
        assertEquals("478758", m.getAccountNumber());
        assertEquals(date, m.getDate());
        assertEquals("Deposito", m.getType());
        assertEquals(new BigDecimal("600.00"), m.getValue());
        assertEquals(new BigDecimal("2600.00"), m.getBalance());
        assertEquals("Cash deposit", m.getDetail());
        assertEquals("idem-001", m.getIdempotencyKey());
    }

    @Test
    void builder_shouldBuildMovementCorrectly() {
        UUID id = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
        LocalDate date = LocalDate.of(2025, 3, 1);

        Movement m = Movement.builder()
                .id(id)
                .accountNumber("ACC-001")
                .date(date)
                .type("Retiro")
                .value(new BigDecimal("50.00"))
                .balance(new BigDecimal("150.00"))
                .detail("ATM withdrawal")
                .idempotencyKey("idem-xyz")
                .build();

        assertEquals(id, m.getId());
        assertEquals("ACC-001", m.getAccountNumber());
        assertEquals(date, m.getDate());
        assertEquals("Retiro", m.getType());
        assertEquals(new BigDecimal("50.00"), m.getValue());
        assertEquals(new BigDecimal("150.00"), m.getBalance());
        assertEquals("ATM withdrawal", m.getDetail());
        assertEquals("idem-xyz", m.getIdempotencyKey());
    }

    @Test
    void toBuilder_shouldCopyValuesAndAllowOverride() {
        UUID id = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
        LocalDate date = LocalDate.of(2025, 2, 10);

        Movement original = Movement.builder()
                .id(id)
                .accountNumber("478758")
                .date(date)
                .type("Deposito")
                .value(new BigDecimal("600.00"))
                .balance(new BigDecimal("2600.00"))
                .detail("Payroll")
                .idempotencyKey("idem-123")
                .build();

        Movement modified = original.toBuilder()
                .detail("Updated detail")
                .balance(new BigDecimal("2700.00"))
                .build();

        assertEquals(id, modified.getId());
        assertEquals("478758", modified.getAccountNumber());
        assertEquals(date, modified.getDate());
        assertEquals("Deposito", modified.getType());
        assertEquals(new BigDecimal("600.00"), modified.getValue());
        assertEquals(new BigDecimal("2700.00"), modified.getBalance());
        assertEquals("Updated detail", modified.getDetail());
        assertEquals("idem-123", modified.getIdempotencyKey());

        assertEquals("Payroll", original.getDetail());
        assertEquals(new BigDecimal("2600.00"), original.getBalance());
    }
}
