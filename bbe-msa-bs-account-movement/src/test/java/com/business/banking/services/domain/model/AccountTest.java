package com.business.banking.services.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AccountTest {

    @Test
    void noArgsConstructor_shouldInitializeBalanceToZero() {
        Account a = new Account();

        assertNotNull(a.getBalance());
        assertEquals(BigDecimal.ZERO, a.getBalance());
        assertNull(a.getNumber());
        assertNull(a.getType());
        assertNull(a.getState());
        assertNull(a.getCustomerId());
    }

    @Test
    void builder_shouldApplyDefaultBalanceWhenNotProvided() {
        UUID customerId = UUID.fromString("11111111-1111-1111-1111-111111111111");

        Account a = Account.builder()
                .number("ACC-001")
                .type("Ahorro")
                .state(true)
                .customerId(customerId)
                .build();

        assertEquals("ACC-001", a.getNumber());
        assertEquals("Ahorro", a.getType());
        assertTrue(a.getState());
        assertEquals(customerId, a.getCustomerId());
        assertEquals(BigDecimal.ZERO, a.getBalance());
    }

    @Test
    void builder_shouldKeepProvidedBalance() {
        Account a = Account.builder()
                .number("ACC-002")
                .type("Corriente")
                .balance(new BigDecimal("25.50"))
                .state(false)
                .build();

        assertEquals("ACC-002", a.getNumber());
        assertEquals("Corriente", a.getType());
        assertFalse(a.getState());
        assertEquals(new BigDecimal("25.50"), a.getBalance());
    }

    @Test
    void toBuilder_shouldCopyValuesAndAllowOverride() {
        UUID customerId = UUID.fromString("22222222-2222-2222-2222-222222222222");

        Account original = Account.builder()
                .number("ACC-003")
                .type("Ahorro")
                .balance(new BigDecimal("100.00"))
                .state(true)
                .customerId(customerId)
                .build();

        Account modified = original.toBuilder()
                .balance(new BigDecimal("80.00"))
                .state(false)
                .build();

        assertEquals("ACC-003", modified.getNumber());
        assertEquals("Ahorro", modified.getType());
        assertEquals(new BigDecimal("80.00"), modified.getBalance());
        assertFalse(modified.getState());
        assertEquals(customerId, modified.getCustomerId());

        assertEquals(new BigDecimal("100.00"), original.getBalance());
        assertTrue(original.getState());
    }
}
