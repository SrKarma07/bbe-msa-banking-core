package com.business.banking.services.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MovementTypeTest {

    @Test
    void apiValue_shouldReturnExpectedApiValue() {
        assertEquals("Deposito", MovementType.DEPOSIT.apiValue());
        assertEquals("Retiro", MovementType.WITHDRAWAL.apiValue());
    }

    @Test
    void fromApiValue_whenValidExact_shouldReturnEnum() {
        assertEquals(MovementType.DEPOSIT, MovementType.fromApiValue("Deposito"));
        assertEquals(MovementType.WITHDRAWAL, MovementType.fromApiValue("Retiro"));
    }

    @Test
    void fromApiValue_whenValidDifferentCase_shouldReturnEnum() {
        assertEquals(MovementType.DEPOSIT, MovementType.fromApiValue("deposito"));
        assertEquals(MovementType.WITHDRAWAL, MovementType.fromApiValue("rEtIrO"));
    }

    @Test
    void fromApiValue_whenValidWithSpaces_shouldTrimAndReturnEnum() {
        assertEquals(MovementType.DEPOSIT, MovementType.fromApiValue("  Deposito  "));
        assertEquals(MovementType.WITHDRAWAL, MovementType.fromApiValue("   Retiro"));
    }

    @Test
    void fromApiValue_whenNull_shouldThrowIllegalArgumentException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> MovementType.fromApiValue(null));

        assertEquals("type is required", ex.getMessage());
    }

    @Test
    void fromApiValue_whenBlank_shouldThrowIllegalArgumentException() {
        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class,
                () -> MovementType.fromApiValue(""));

        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class,
                () -> MovementType.fromApiValue("   "));

        assertEquals("type is required", ex1.getMessage());
        assertEquals("type is required", ex2.getMessage());
    }

    @Test
    void fromApiValue_whenInvalid_shouldThrowIllegalArgumentExceptionWithRawValue() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> MovementType.fromApiValue("Transfer"));

        assertEquals("invalid type: Transfer", ex.getMessage());
    }

    @Test
    void fromApiValue_whenInvalidWithSpaces_shouldThrowIllegalArgumentExceptionWithRawValue() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> MovementType.fromApiValue("  Transfer  "));

        assertEquals("invalid type:   Transfer  ", ex.getMessage());
    }
}
