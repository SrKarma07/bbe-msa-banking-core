package com.business.banking.services.infrastructure.input.adapter.rest.mapper;

import com.business.banking.services.domain.model.Movement;
import com.business.banking.services.infrastructure.input.adapter.rest.dto.MovementCreateRequest;
import com.business.banking.services.infrastructure.input.adapter.rest.dto.MovementResponse;
import com.business.banking.services.infrastructure.input.adapter.rest.dto.MovementUpdateRequest;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class MovementControllerMapperTest {

    private final MovementControllerMapper mapper = Mappers.getMapper(MovementControllerMapper.class);

    @Test
    void toDomain_shouldMapCreateRequestToMovement_andIgnoreServerFields() {
        MovementCreateRequest req = new MovementCreateRequest()
                .accountNumber("ACC-1")
                .date(LocalDate.of(2025, 2, 10))
                .type("Deposito")
                .value(new BigDecimal("50.00"))
                .detail("Cash deposit");

        Movement m = mapper.toDomain(req);

        assertNotNull(m);
        assertNull(m.getId());
        assertEquals("ACC-1", m.getAccountNumber());
        assertEquals(LocalDate.of(2025, 2, 10), m.getDate());
        assertEquals("Deposito", m.getType());
        assertEquals(new BigDecimal("50.00"), m.getValue());
        assertEquals("Cash deposit", m.getDetail());
        assertNull(m.getBalance());
        assertNull(m.getIdempotencyKey());
    }

    @Test
    void toPatch_shouldMapOnlyAllowedFields_andIgnoreOthers() {
        MovementUpdateRequest req = new MovementUpdateRequest()
                .date(LocalDate.of(2025, 2, 11))
                .type("Retiro")
                .value(new BigDecimal("15.25"))
                .detail("ATM withdrawal");

        Movement patch = mapper.toPatch(req);

        assertNotNull(patch);
        assertNull(patch.getId());
        assertNull(patch.getAccountNumber());
        assertEquals(LocalDate.of(2025, 2, 11), patch.getDate());
        assertEquals("Retiro", patch.getType());
        assertEquals(new BigDecimal("15.25"), patch.getValue());
        assertEquals("ATM withdrawal", patch.getDetail());
        assertNull(patch.getBalance());
        assertNull(patch.getIdempotencyKey());
    }

    @Test
    void toPatch_whenAllNull_shouldReturnMovementWithNulls() {
        MovementUpdateRequest req = new MovementUpdateRequest();

        Movement patch = mapper.toPatch(req);

        assertNotNull(patch);
        assertNull(patch.getId());
        assertNull(patch.getAccountNumber());
        assertNull(patch.getDate());
        assertNull(patch.getType());
        assertNull(patch.getValue());
        assertNull(patch.getDetail());

        // IMPORTANT: balance is ignored -> stays null
        assertNull(patch.getBalance());

        // IMPORTANT: idempotencyKey is ignored -> stays null
        assertNull(patch.getIdempotencyKey());
    }

    @Test
    void toResponse_shouldMapMovementToResponse() {
        UUID id = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

        Movement m = Movement.builder()
                .id(id)
                .accountNumber("ACC-9")
                .date(LocalDate.of(2025, 2, 12))
                .type("Deposito")
                .value(new BigDecimal("100.00"))
                .balance(new BigDecimal("250.00"))
                .detail("Payroll")
                .idempotencyKey("k-1")
                .build();

        MovementResponse res = mapper.toResponse(m);

        assertNotNull(res);
        assertEquals(id, res.getId());
        assertEquals("ACC-9", res.getAccountNumber());
        assertEquals(LocalDate.of(2025, 2, 12), res.getDate());
        assertEquals("Deposito", res.getType());
        assertEquals(new BigDecimal("100.00"), res.getValue());
        assertEquals(new BigDecimal("250.00"), res.getBalance());
    }

    @Test
    void toDomain_whenNull_shouldReturnNull() {
        assertNull(mapper.toDomain(null));
    }

    @Test
    void toPatch_whenNull_shouldReturnNull() {
        assertNull(mapper.toPatch(null));
    }

    @Test
    void toResponse_whenNull_shouldReturnNull() {
        assertNull(mapper.toResponse(null));
    }
}
