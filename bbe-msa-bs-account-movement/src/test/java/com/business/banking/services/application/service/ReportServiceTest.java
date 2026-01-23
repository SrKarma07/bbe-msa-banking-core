package com.business.banking.services.application.service;

import com.business.banking.services.application.output.port.AccountRepositoryPort;
import com.business.banking.services.application.output.port.MovementRepositoryPort;
import com.business.banking.services.domain.exception.InvalidRequestException;
import com.business.banking.services.domain.model.Account;
import com.business.banking.services.domain.model.AccountStatement;
import com.business.banking.services.domain.model.Movement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private AccountRepositoryPort accountRepo;

    @Mock
    private MovementRepositoryPort movementRepo;

    @InjectMocks
    private ReportService service;

    private UUID customerId;
    private String customerIdStr;

    @BeforeEach
    void setup() {
        customerId = UUID.randomUUID();
        customerIdStr = customerId.toString();
    }

    @Test
    void getAccountStatement_whenCustomerIdBlank_shouldErrorInvalidRequest() {
        StepVerifier.create(service.getAccountStatement("  ", LocalDate.now(), LocalDate.now()))
                .expectError(InvalidRequestException.class)
                .verify();

        verifyNoInteractions(accountRepo, movementRepo);
    }

    @Test
    void getAccountStatement_whenDatesNull_shouldErrorInvalidRequest() {
        StepVerifier.create(service.getAccountStatement(customerIdStr, null, LocalDate.now()))
                .expectError(InvalidRequestException.class)
                .verify();

        StepVerifier.create(service.getAccountStatement(customerIdStr, LocalDate.now(), null))
                .expectError(InvalidRequestException.class)
                .verify();

        verifyNoInteractions(accountRepo, movementRepo);
    }

    @Test
    void getAccountStatement_whenFromAfterTo_shouldErrorInvalidRequest() {
        LocalDate from = LocalDate.of(2025, 2, 10);
        LocalDate to = LocalDate.of(2025, 2, 1);

        StepVerifier.create(service.getAccountStatement(customerIdStr, from, to))
                .expectError(InvalidRequestException.class)
                .verify();

        verifyNoInteractions(accountRepo, movementRepo);
    }

    @Test
    void getAccountStatement_whenCustomerIdNotUuid_shouldErrorInvalidRequest() {
        StepVerifier.create(service.getAccountStatement("not-a-uuid", LocalDate.now(), LocalDate.now()))
                .expectError(InvalidRequestException.class)
                .verify();

        verifyNoInteractions(accountRepo, movementRepo);
    }

    @Test
    void getAccountStatement_whenNoAccounts_shouldReturnEmptyStatement_andNotQueryMovements() {
        LocalDate from = LocalDate.of(2025, 1, 1);
        LocalDate to = LocalDate.of(2025, 1, 31);

        when(accountRepo.findByCustomerId(customerId)).thenReturn(Flux.empty());

        StepVerifier.create(service.getAccountStatement(customerIdStr, from, to))
                .assertNext(stmt -> {
                    assertNotNull(stmt);
                    assertEquals(customerIdStr, stmt.getCustomerId());
                    assertEquals(from, stmt.getDateFrom());
                    assertEquals(to, stmt.getDateTo());
                    assertNotNull(stmt.getAccounts());
                    assertTrue(stmt.getAccounts().isEmpty());
                })
                .verifyComplete();

        verify(accountRepo).findByCustomerId(customerId);
        verifyNoInteractions(movementRepo);
    }

    @Test
    void getAccountStatement_whenAccountsExist_shouldReturnSummariesWithMovementsGrouped() {
        LocalDate from = LocalDate.of(2025, 1, 1);
        LocalDate to = LocalDate.of(2025, 1, 31);

        Account a1 = Account.builder()
                .number("001")
                .type("SAVINGS")
                .state(true)
                .balance(new BigDecimal("100.00"))
                .customerId(customerId)
                .build();

        Account a2 = Account.builder()
                .number("002")
                .type("CURRENT")
                .state(false)
                .balance(new BigDecimal("50.00"))
                .customerId(customerId)
                .build();

        Movement m1 = Movement.builder()
                .accountNumber("001")
                .date(LocalDate.of(2025, 1, 10))
                .type("Deposito")
                .value(new BigDecimal("10.00"))
                .balance(new BigDecimal("110.00"))
                .build();

        Movement m2 = Movement.builder()
                .accountNumber("001")
                .date(LocalDate.of(2025, 1, 11))
                .type("Retiro")
                .value(new BigDecimal("5.00"))
                .balance(new BigDecimal("105.00"))
                .build();

        when(accountRepo.findByCustomerId(customerId)).thenReturn(Flux.just(a1, a2));
        when(movementRepo.findByAccountNumbersAndDateRange(List.of("001", "002"), from, to))
                .thenReturn(Flux.just(m1, m2));

        StepVerifier.create(service.getAccountStatement(customerIdStr, from, to))
                .assertNext(stmt -> {
                    assertEquals(customerIdStr, stmt.getCustomerId());
                    assertEquals(from, stmt.getDateFrom());
                    assertEquals(to, stmt.getDateTo());

                    assertNotNull(stmt.getAccounts());
                    assertEquals(2, stmt.getAccounts().size());

                    var s1 = stmt.getAccounts().stream()
                            .filter(s -> "001".equals(s.getAccountNumber()))
                            .findFirst()
                            .orElseThrow();

                    assertEquals("SAVINGS", s1.getType());
                    assertTrue(s1.getState());
                    assertEquals(new BigDecimal("100.00"), s1.getBalance());
                    assertNotNull(s1.getMovements());
                    assertEquals(2, s1.getMovements().size());

                    var d1 = s1.getMovements().get(0);
                    assertEquals(LocalDate.of(2025, 1, 10), d1.getDate());
                    assertEquals("Deposito", d1.getType());
                    assertEquals(new BigDecimal("10.00"), d1.getValue());
                    assertEquals(new BigDecimal("110.00"), d1.getBalance());

                    var d2 = s1.getMovements().get(1);
                    assertEquals(LocalDate.of(2025, 1, 11), d2.getDate());
                    assertEquals("Retiro", d2.getType());
                    assertEquals(new BigDecimal("5.00"), d2.getValue());
                    assertEquals(new BigDecimal("105.00"), d2.getBalance());

                    var s2 = stmt.getAccounts().stream()
                            .filter(s -> "002".equals(s.getAccountNumber()))
                            .findFirst()
                            .orElseThrow();

                    assertEquals("CURRENT", s2.getType());
                    assertFalse(s2.getState());
                    assertEquals(new BigDecimal("50.00"), s2.getBalance());
                    assertNotNull(s2.getMovements());
                    assertEquals(0, s2.getMovements().size());
                })
                .verifyComplete();

        verify(accountRepo).findByCustomerId(customerId);
        verify(movementRepo).findByAccountNumbersAndDateRange(List.of("001", "002"), from, to);
    }
}
