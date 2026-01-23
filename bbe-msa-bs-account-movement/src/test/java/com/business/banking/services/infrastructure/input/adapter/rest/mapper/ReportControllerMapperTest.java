package com.business.banking.services.infrastructure.input.adapter.rest.mapper;

import com.business.banking.services.domain.model.AccountStatement;
import com.business.banking.services.domain.model.AccountSummary;
import com.business.banking.services.domain.model.MovementDetail;
import com.business.banking.services.infrastructure.input.adapter.rest.dto.AccountResponse;
import com.business.banking.services.infrastructure.input.adapter.rest.dto.AccountStatementResponse;
import com.business.banking.services.infrastructure.input.adapter.rest.dto.AccountStatementResponseAccountsInner;
import com.business.banking.services.infrastructure.input.adapter.rest.dto.MovementResponse;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ReportControllerMapperTest {

    private final ReportControllerMapper mapper = Mappers.getMapper(ReportControllerMapper.class);

    @Test
    void toResponse_shouldMapAccountStatementWithAccountsAndMovements() {
        UUID customerId = UUID.fromString("11111111-1111-1111-1111-111111111111");

        MovementDetail d1 = MovementDetail.builder()
                .date(LocalDate.of(2025, 1, 10))
                .type("Deposito")
                .value(new BigDecimal("100.00"))
                .balance(new BigDecimal("1100.00"))
                .build();

        AccountSummary acc1 = AccountSummary.builder()
                .accountNumber("ACC-001")
                .type("Ahorro")
                .state(true)
                .balance(new BigDecimal("1100.00"))
                .movements(List.of(d1))
                .build();

        AccountStatement report = AccountStatement.builder()
                .customerId(customerId.toString())
                .dateFrom(LocalDate.of(2025, 1, 1))
                .dateTo(LocalDate.of(2025, 1, 31))
                .accounts(List.of(acc1))
                .build();

        AccountStatementResponse res = mapper.toResponse(report);

        assertNotNull(res);

        // top level
        assertEquals(customerId, res.getCustomerId());
        assertEquals(LocalDate.of(2025, 1, 1), res.getDateFrom());
        assertEquals(LocalDate.of(2025, 1, 31), res.getDateTo());

        assertNotNull(res.getAccounts());
        assertEquals(1, res.getAccounts().size());

        AccountStatementResponseAccountsInner inner = res.getAccounts().get(0);
        assertNotNull(inner);

        // account inside
        assertNotNull(inner.getAccount());
        AccountResponse ar = inner.getAccount();

        assertEquals("ACC-001", ar.getAccountNumber());
        assertEquals("Ahorro", ar.getType());
        assertTrue(ar.getState());
        assertEquals(new BigDecimal("1100.00"), ar.getBalance());
        assertEquals(customerId, ar.getCustomerId());

        // movements
        assertNotNull(inner.getMovements());
        assertEquals(1, inner.getMovements().size());

        MovementResponse mr = inner.getMovements().get(0);
        assertNotNull(mr);

        // IMPORTANT: id is random UUID -> just validate it exists
        assertNotNull(mr.getId());

        assertEquals("ACC-001", mr.getAccountNumber());
        assertEquals(LocalDate.of(2025, 1, 10), mr.getDate());
        assertEquals("Deposito", mr.getType());
        assertEquals(new BigDecimal("100.00"), mr.getValue());
        assertEquals(new BigDecimal("1100.00"), mr.getBalance());
    }

    @Test
    void toResponse_whenAccountsNull_shouldReturnEmptyAccountsList() {
        UUID customerId = UUID.fromString("22222222-2222-2222-2222-222222222222");

        AccountStatement report = AccountStatement.builder()
                .customerId(customerId.toString())
                .dateFrom(LocalDate.of(2025, 2, 1))
                .dateTo(LocalDate.of(2025, 2, 28))
                .accounts(null)
                .build();

        AccountStatementResponse res = mapper.toResponse(report);

        assertNotNull(res);
        assertEquals(customerId, res.getCustomerId());
        assertEquals(LocalDate.of(2025, 2, 1), res.getDateFrom());
        assertEquals(LocalDate.of(2025, 2, 28), res.getDateTo());

        assertNotNull(res.getAccounts());
        assertTrue(res.getAccounts().isEmpty());
    }

    @Test
    void toResponse_whenMovementsNull_shouldReturnInnerWithEmptyMovements() {
        UUID customerId = UUID.fromString("33333333-3333-3333-3333-333333333333");

        AccountSummary acc1 = AccountSummary.builder()
                .accountNumber("ACC-NULL-MOVS")
                .type("Corriente")
                .state(false)
                .balance(new BigDecimal("0.00"))
                .movements(null) // IMPORTANT
                .build();

        AccountStatement report = AccountStatement.builder()
                .customerId(customerId.toString())
                .dateFrom(LocalDate.of(2025, 3, 1))
                .dateTo(LocalDate.of(2025, 3, 31))
                .accounts(List.of(acc1))
                .build();

        AccountStatementResponse res = mapper.toResponse(report);

        assertNotNull(res);
        assertNotNull(res.getAccounts());
        assertEquals(1, res.getAccounts().size());

        AccountStatementResponseAccountsInner inner = res.getAccounts().get(0);
        assertNotNull(inner);

        assertNotNull(inner.getMovements());
        assertTrue(inner.getMovements().isEmpty());
    }

    @Test
    void toAccountResponse_shouldMapAccountSummaryToAccountResponse() {
        UUID customerId = UUID.fromString("44444444-4444-4444-4444-444444444444");

        AccountSummary item = AccountSummary.builder()
                .accountNumber("ACC-777")
                .type("Ahorro")
                .state(true)
                .balance(new BigDecimal("999.99"))
                .build();

        AccountResponse res = mapper.toAccountResponse(item, customerId);

        assertNotNull(res);
        assertEquals("ACC-777", res.getAccountNumber());
        assertEquals("Ahorro", res.getType());
        assertTrue(res.getState());
        assertEquals(new BigDecimal("999.99"), res.getBalance());
        assertEquals(customerId, res.getCustomerId());
    }

    @Test
    void toMovementResponse_shouldMapMovementDetailToMovementResponse() {
        UUID id = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

        MovementDetail detail = MovementDetail.builder()
                .date(LocalDate.of(2025, 5, 5))
                .type("Retiro")
                .value(new BigDecimal("25.00"))
                .balance(new BigDecimal("75.00"))
                .build();

        MovementResponse res = mapper.toMovementResponse(id, "ACC-XYZ", detail);

        assertNotNull(res);
        assertEquals(id, res.getId());
        assertEquals("ACC-XYZ", res.getAccountNumber());
        assertEquals(LocalDate.of(2025, 5, 5), res.getDate());
        assertEquals("Retiro", res.getType());
        assertEquals(new BigDecimal("25.00"), res.getValue());
        assertEquals(new BigDecimal("75.00"), res.getBalance());
    }

    @Test
    void toResponse_whenNull_shouldReturnNull() {
        assertNull(mapper.toResponse(null));
    }
}
