package com.business.banking.services.infrastructure.input.adapter.rest.mapper;

import com.business.banking.services.domain.model.Account;
import com.business.banking.services.infrastructure.input.adapter.rest.dto.AccountCreateRequest;
import com.business.banking.services.infrastructure.input.adapter.rest.dto.AccountResponse;
import com.business.banking.services.infrastructure.input.adapter.rest.dto.AccountUpdateRequest;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AccountControllerMapperTest {

    private final AccountControllerMapper mapper = Mappers.getMapper(AccountControllerMapper.class);

    @Test
    void toDomain_shouldMapCreateRequestToAccount() {
        UUID customerId = UUID.fromString("11111111-1111-1111-1111-111111111111");

        AccountCreateRequest req = new AccountCreateRequest()
                .accountNumber("ACC-1")
                .type("Ahorro")
                .initialBalance(new BigDecimal("2000.00"))
                .state(true)
                .customerId(customerId);

        Account a = mapper.toDomain(req);

        assertNotNull(a);
        assertEquals("ACC-1", a.getNumber());
        assertEquals("Ahorro", a.getType());
        assertEquals(new BigDecimal("2000.00"), a.getBalance());
        assertTrue(a.getState());
        assertEquals(customerId, a.getCustomerId());
    }

    @Test
    void toPatch_shouldMapOnlyTypeAndState_andIgnoreOthers() {
        AccountUpdateRequest req = new AccountUpdateRequest()
                .type("Corriente")
                .state(false);

        Account patch = mapper.toPatch(req);

        assertNotNull(patch);
        assertEquals("Corriente", patch.getType());
        assertFalse(patch.getState());

        assertNull(patch.getNumber());
        assertEquals(BigDecimal.ZERO, patch.getBalance());
        assertNull(patch.getCustomerId());
    }

    @Test
    void toPatch_whenAllNull_shouldReturnAccountWithDefaults() {
        AccountUpdateRequest req = new AccountUpdateRequest();

        Account patch = mapper.toPatch(req);

        assertNotNull(patch);
        assertNull(patch.getType());
        assertNull(patch.getState());

        assertNull(patch.getNumber());
        assertEquals(BigDecimal.ZERO, patch.getBalance());
        assertNull(patch.getCustomerId());
    }

    @Test
    void toResponse_shouldMapAccountToResponse() {
        UUID customerId = UUID.fromString("22222222-2222-2222-2222-222222222222");

        Account a = Account.builder()
                .number("ACC-9")
                .type("Ahorro")
                .state(true)
                .balance(new BigDecimal("150.25"))
                .customerId(customerId)
                .build();

        AccountResponse res = mapper.toResponse(a);

        assertNotNull(res);
        assertEquals("ACC-9", res.getAccountNumber());
        assertEquals("Ahorro", res.getType());
        assertTrue(res.getState());
        assertEquals(new BigDecimal("150.25"), res.getBalance());
        assertEquals(customerId, res.getCustomerId());
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
