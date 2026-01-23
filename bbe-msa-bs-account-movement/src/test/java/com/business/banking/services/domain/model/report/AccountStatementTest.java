package com.business.banking.services.domain.model.report;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AccountStatementTest {

    @Test
    void noArgsConstructor_shouldCreateEmptyAccountStatement() {
        AccountStatement r = new AccountStatement();

        assertNull(r.getCustomerId());
        assertNull(r.getFrom());
        assertNull(r.getTo());
        assertNull(r.getAccounts());
    }

    @Test
    void allArgsConstructor_shouldSetAllFields() {
        AccountStatement.MovementDetail d1 = new AccountStatement.MovementDetail(
                LocalDate.of(2025, 2, 10),
                "Deposito",
                new BigDecimal("100.00"),
                new BigDecimal("1100.00")
        );

        AccountStatement.AccountSummary a1 = new AccountStatement.AccountSummary(
                "478758",
                "Ahorro",
                new BigDecimal("1100.00"),
                List.of(d1)
        );

        AccountStatement r = new AccountStatement(
                "11111111-1111-1111-1111-111111111111",
                LocalDate.of(2025, 2, 1),
                LocalDate.of(2025, 2, 28),
                List.of(a1)
        );

        assertEquals("11111111-1111-1111-1111-111111111111", r.getCustomerId());
        assertEquals(LocalDate.of(2025, 2, 1), r.getFrom());
        assertEquals(LocalDate.of(2025, 2, 28), r.getTo());
        assertNotNull(r.getAccounts());
        assertEquals(1, r.getAccounts().size());

        AccountStatement.AccountSummary acc = r.getAccounts().get(0);
        assertEquals("478758", acc.getAccountNumber());
        assertEquals("Ahorro", acc.getType());
        assertEquals(new BigDecimal("1100.00"), acc.getCurrentBalance());
        assertNotNull(acc.getMovements());
        assertEquals(1, acc.getMovements().size());

        AccountStatement.MovementDetail md = acc.getMovements().get(0);
        assertEquals(LocalDate.of(2025, 2, 10), md.getDate());
        assertEquals("Deposito", md.getType());
        assertEquals(new BigDecimal("100.00"), md.getValue());
        assertEquals(new BigDecimal("1100.00"), md.getBalance());
    }

    @Test
    void builder_shouldBuildAccountStatementWithNestedObjects() {
        AccountStatement.MovementDetail d1 = AccountStatement.MovementDetail.builder()
                .date(LocalDate.of(2025, 3, 5))
                .type("Retiro")
                .value(new BigDecimal("50.00"))
                .balance(new BigDecimal("950.00"))
                .build();

        AccountStatement.AccountSummary a1 = AccountStatement.AccountSummary.builder()
                .accountNumber("ACC-001")
                .type("Corriente")
                .currentBalance(new BigDecimal("950.00"))
                .movements(List.of(d1))
                .build();

        AccountStatement r = AccountStatement.builder()
                .customerId("22222222-2222-2222-2222-222222222222")
                .from(LocalDate.of(2025, 3, 1))
                .to(LocalDate.of(2025, 3, 31))
                .accounts(List.of(a1))
                .build();

        assertEquals("22222222-2222-2222-2222-222222222222", r.getCustomerId());
        assertEquals(LocalDate.of(2025, 3, 1), r.getFrom());
        assertEquals(LocalDate.of(2025, 3, 31), r.getTo());
        assertNotNull(r.getAccounts());
        assertEquals(1, r.getAccounts().size());

        AccountStatement.AccountSummary acc = r.getAccounts().get(0);
        assertEquals("ACC-001", acc.getAccountNumber());
        assertEquals("Corriente", acc.getType());
        assertEquals(new BigDecimal("950.00"), acc.getCurrentBalance());
        assertNotNull(acc.getMovements());
        assertEquals(1, acc.getMovements().size());

        AccountStatement.MovementDetail md = acc.getMovements().get(0);
        assertEquals(LocalDate.of(2025, 3, 5), md.getDate());
        assertEquals("Retiro", md.getType());
        assertEquals(new BigDecimal("50.00"), md.getValue());
        assertEquals(new BigDecimal("950.00"), md.getBalance());
    }

    @Test
    void toBuilder_shouldCopyAndAllowOverride_inRootAndNestedObjects() {
        AccountStatement.MovementDetail d1 = AccountStatement.MovementDetail.builder()
                .date(LocalDate.of(2025, 2, 10))
                .type("Deposito")
                .value(new BigDecimal("100.00"))
                .balance(new BigDecimal("1100.00"))
                .build();

        AccountStatement.AccountSummary a1 = AccountStatement.AccountSummary.builder()
                .accountNumber("478758")
                .type("Ahorro")
                .currentBalance(new BigDecimal("1100.00"))
                .movements(List.of(d1))
                .build();

        AccountStatement original = AccountStatement.builder()
                .customerId("33333333-3333-3333-3333-333333333333")
                .from(LocalDate.of(2025, 2, 1))
                .to(LocalDate.of(2025, 2, 28))
                .accounts(List.of(a1))
                .build();

        AccountStatement modified = original.toBuilder()
                .to(LocalDate.of(2025, 3, 1))
                .build();

        assertEquals("33333333-3333-3333-3333-333333333333", modified.getCustomerId());
        assertEquals(LocalDate.of(2025, 2, 1), modified.getFrom());
        assertEquals(LocalDate.of(2025, 3, 1), modified.getTo());
        assertNotNull(modified.getAccounts());
        assertEquals(1, modified.getAccounts().size());

        AccountStatement.AccountSummary acc = modified.getAccounts().get(0);
        assertEquals("478758", acc.getAccountNumber());
        assertEquals("Ahorro", acc.getType());
        assertEquals(new BigDecimal("1100.00"), acc.getCurrentBalance());
        assertNotNull(acc.getMovements());
        assertEquals(1, acc.getMovements().size());

        assertEquals(LocalDate.of(2025, 2, 28), original.getTo());
    }
}
