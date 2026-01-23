package com.business.banking.services.application.service;

import com.business.banking.services.application.output.port.AccountRepositoryPort;
import com.business.banking.services.application.output.port.MovementRepositoryPort;
import com.business.banking.services.application.shared.PageResult;
import com.business.banking.services.domain.exception.AccountClosedException;
import com.business.banking.services.domain.exception.InsufficientBalanceException;
import com.business.banking.services.domain.exception.InvalidRequestException;
import com.business.banking.services.domain.exception.NotFoundException;
import com.business.banking.services.domain.model.Account;
import com.business.banking.services.domain.model.Movement;
import com.business.banking.services.domain.model.MovementType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class MovementServiceTest {

    @Mock
    private AccountRepositoryPort accountRepo;

    @Mock
    private MovementRepositoryPort movementRepo;

    @Mock
    private TransactionalOperator tx;

    @InjectMocks
    private MovementService service;

    @BeforeEach
    void setup() {
        lenient().when(tx.transactional(Mockito.<Mono<?>>any()))
                .thenAnswer(inv -> inv.getArgument(0));

        lenient().when(tx.transactional(Mockito.<Flux<?>>any()))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void list_shouldDelegateToRepo() {
        Movement m1 = Movement.builder().id(UUID.randomUUID()).build();
        Movement m2 = Movement.builder().id(UUID.randomUUID()).build();

        when(movementRepo.findAll()).thenReturn(Flux.just(m1, m2));

        StepVerifier.create(service.list())
                .expectNext(m1)
                .expectNext(m2)
                .verifyComplete();

        verify(movementRepo).findAll();
        verifyNoMoreInteractions(movementRepo);
        verifyNoInteractions(accountRepo);
    }

    @Test
    void getById_whenNull_shouldThrowImmediately() {
        assertThrows(NullPointerException.class, () -> service.getById(null));
        verifyNoInteractions(accountRepo, movementRepo);
    }

    @Test
    void getById_whenFound_shouldReturnMovement() {
        UUID id = UUID.randomUUID();
        Movement found = Movement.builder().id(id).build();

        when(movementRepo.findById(id)).thenReturn(Mono.just(found));

        StepVerifier.create(service.getById(id))
                .expectNext(found)
                .verifyComplete();

        verify(movementRepo).findById(id);
        verifyNoInteractions(accountRepo);
    }

    @Test
    void getById_whenNotFound_shouldErrorNotFoundException() {
        UUID id = UUID.randomUUID();
        when(movementRepo.findById(id)).thenReturn(Mono.empty());

        StepVerifier.create(service.getById(id))
                .expectError(NotFoundException.class)
                .verify();

        verify(movementRepo).findById(id);
        verifyNoInteractions(accountRepo);
    }

    @Test
    void create_whenInvalidMovement_shouldErrorAndNotCallRepos() {
        StepVerifier.create(service.create(null, null))
                .expectError(InvalidRequestException.class)
                .verify();

        verifyNoInteractions(accountRepo, movementRepo);
    }

    @Test
    void create_whenIdempotencyKeyBlank_shouldBehaveAsNoKey_andCreateMovement() {
        String accNumber = "ACC-1";
        LocalDate date = LocalDate.now();

        Account acc = Account.builder()
                .number(accNumber)
                .balance(new BigDecimal("100.00"))
                .state(true)
                .build();

        Movement req = Movement.builder()
                .accountNumber(accNumber)
                .date(date)
                .type(MovementType.DEPOSIT.apiValue())
                .value(new BigDecimal("25.00"))
                .detail(" deposit ")
                .build();

        when(accountRepo.findByNumber(accNumber)).thenReturn(Mono.just(acc));
        when(movementRepo.save(any(Movement.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(accountRepo.updateBalance(eq(accNumber), any(BigDecimal.class))).thenReturn(Mono.empty());

        StepVerifier.create(service.create(req, "   "))
                .assertNext(saved -> {
                    assertEquals(accNumber, saved.getAccountNumber());
                    assertEquals(new BigDecimal("25.00"), saved.getValue());
                    assertEquals(new BigDecimal("125.00"), saved.getBalance());
                    assertNull(saved.getIdempotencyKey());
                })
                .verifyComplete();

        verify(accountRepo).findByNumber(accNumber);
        verify(movementRepo).save(any(Movement.class));
        verify(accountRepo).updateBalance(accNumber, new BigDecimal("125.00"));
        verify(tx).transactional(any(Mono.class));
    }

    @Test
    void create_whenNoKey_andAccountNotFound_shouldErrorNotFound() {
        Movement req = Movement.builder()
                .accountNumber("ACC-X")
                .date(LocalDate.now())
                .type(MovementType.DEPOSIT.apiValue())
                .value(new BigDecimal("10.00"))
                .build();

        when(accountRepo.findByNumber("ACC-X")).thenReturn(Mono.empty());

        StepVerifier.create(service.create(req, null))
                .expectError(NotFoundException.class)
                .verify();

        verify(accountRepo).findByNumber("ACC-X");
        verifyNoInteractions(movementRepo);
    }

    @Test
    void create_whenNoKey_andAccountClosed_shouldErrorAccountClosed() {
        String accNumber = "ACC-2";

        Account closed = Account.builder()
                .number(accNumber)
                .balance(new BigDecimal("50.00"))
                .state(false)
                .build();

        Movement req = Movement.builder()
                .accountNumber(accNumber)
                .date(LocalDate.now())
                .type(MovementType.DEPOSIT.apiValue())
                .value(new BigDecimal("10.00"))
                .build();

        when(accountRepo.findByNumber(accNumber)).thenReturn(Mono.just(closed));

        StepVerifier.create(service.create(req, null))
                .expectError(AccountClosedException.class)
                .verify();

        verify(accountRepo).findByNumber(accNumber);
        verify(tx).transactional(any(Mono.class));
        verifyNoInteractions(movementRepo);
        verify(accountRepo, never()).updateBalance(anyString(), any());
    }

    @Test
    void create_whenNoKey_andInvalidType_shouldErrorInvalidRequest() {
        Movement req = Movement.builder()
                .accountNumber("ACC-3")
                .date(LocalDate.now())
                .type("BAD_TYPE")
                .value(new BigDecimal("10.00"))
                .build();

        StepVerifier.create(service.create(req, null))
                .expectError(InvalidRequestException.class)
                .verify();

        verifyNoInteractions(accountRepo, movementRepo);
    }



    @Test
    void create_whenNoKey_andInsufficientBalance_shouldError() {
        String accNumber = "ACC-4";

        Account acc = Account.builder()
                .number(accNumber)
                .balance(new BigDecimal("5.00"))
                .state(true)
                .build();

        Movement req = Movement.builder()
                .accountNumber(accNumber)
                .date(LocalDate.now())
                .type(MovementType.WITHDRAWAL.apiValue())
                .value(new BigDecimal("10.00"))
                .build();

        when(accountRepo.findByNumber(accNumber)).thenReturn(Mono.just(acc));

        StepVerifier.create(service.create(req, null))
                .expectError(InsufficientBalanceException.class)
                .verify();

        verify(accountRepo).findByNumber(accNumber);
        verify(tx).transactional(any(Mono.class));
        verifyNoInteractions(movementRepo);
        verify(accountRepo, never()).updateBalance(anyString(), any());
    }

    @Test
    void create_whenNoKey_deposit_shouldSaveMovementAndUpdateBalance() {
        String accNumber = "ACC-5";

        Account acc = Account.builder()
                .number(accNumber)
                .balance(new BigDecimal("100.00"))
                .state(true)
                .build();

        Movement req = Movement.builder()
                .accountNumber(accNumber)
                .date(LocalDate.now())
                .type(MovementType.DEPOSIT.apiValue())
                .value(new BigDecimal("20.00"))
                .detail("ok")
                .build();

        when(accountRepo.findByNumber(accNumber)).thenReturn(Mono.just(acc));
        when(movementRepo.save(any(Movement.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(accountRepo.updateBalance(eq(accNumber), any(BigDecimal.class))).thenReturn(Mono.empty());

        StepVerifier.create(service.create(req, null))
                .assertNext(saved -> {
                    assertEquals(accNumber, saved.getAccountNumber());
                    assertEquals(new BigDecimal("20.00"), saved.getValue());
                    assertEquals(new BigDecimal("120.00"), saved.getBalance());
                    assertNull(saved.getIdempotencyKey());
                })
                .verifyComplete();

        verify(accountRepo).findByNumber(accNumber);
        verify(movementRepo).save(any(Movement.class));
        verify(accountRepo).updateBalance(accNumber, new BigDecimal("120.00"));
        verify(tx).transactional(any(Mono.class));
    }

    @Test
    void create_whenKeyAlreadyExists_andSamePayload_shouldReturnExistingWithoutSaving() {
        String key = "idem-123";
        String accNumber = "ACC-6";
        LocalDate date = LocalDate.now();

        Movement existing = Movement.builder()
                .id(UUID.randomUUID())
                .accountNumber(accNumber)
                .date(date)
                .type(MovementType.DEPOSIT.apiValue())
                .value(new BigDecimal("10.00"))
                .detail("same")
                .idempotencyKey(key)
                .build();

        Movement requested = Movement.builder()
                .accountNumber(accNumber)
                .date(date)
                .type(MovementType.DEPOSIT.apiValue())
                .value(new BigDecimal("10.00"))
                .detail(" same ")
                .build();

        when(movementRepo.findFirstByIdempotencyKey(key)).thenReturn(Mono.just(existing));

        StepVerifier.create(service.create(requested, key))
                .expectNext(existing)
                .verifyComplete();

        verify(movementRepo).findFirstByIdempotencyKey(key);
        verifyNoInteractions(accountRepo);
        verify(movementRepo, never()).save(any());
    }

    @Test
    void create_whenKeyAlreadyExists_andDifferentPayload_shouldErrorInvalidRequest() {
        String key = "idem-456";
        String accNumber = "ACC-7";
        LocalDate date = LocalDate.now();

        Movement existing = Movement.builder()
                .id(UUID.randomUUID())
                .accountNumber(accNumber)
                .date(date)
                .type(MovementType.DEPOSIT.apiValue())
                .value(new BigDecimal("10.00"))
                .detail("first")
                .idempotencyKey(key)
                .build();

        Movement requested = Movement.builder()
                .accountNumber(accNumber)
                .date(date)
                .type(MovementType.DEPOSIT.apiValue())
                .value(new BigDecimal("99.00"))
                .detail("different")
                .build();

        when(movementRepo.findFirstByIdempotencyKey(key)).thenReturn(Mono.just(existing));

        StepVerifier.create(service.create(requested, key))
                .expectError(InvalidRequestException.class)
                .verify();

        verify(movementRepo).findFirstByIdempotencyKey(key);
        verifyNoInteractions(accountRepo);
        verify(movementRepo, never()).save(any());
    }

    @Test
    void create_whenKeyNotFound_shouldCreateWithKey_andPersistAndUpdateBalance() {
        String key = "idem-new";
        String accNumber = "ACC-8";

        Account acc = Account.builder()
                .number(accNumber)
                .balance(new BigDecimal("100.00"))
                .state(true)
                .build();

        Movement requested = Movement.builder()
                .accountNumber(accNumber)
                .date(LocalDate.now())
                .type(MovementType.WITHDRAWAL.apiValue())
                .value(new BigDecimal("30.00"))
                .detail("withdraw")
                .build();

        when(movementRepo.findFirstByIdempotencyKey(key)).thenReturn(Mono.empty());
        when(accountRepo.findByNumber(accNumber)).thenReturn(Mono.just(acc));
        when(movementRepo.save(any(Movement.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(accountRepo.updateBalance(eq(accNumber), any(BigDecimal.class))).thenReturn(Mono.empty());

        StepVerifier.create(service.create(requested, key))
                .assertNext(saved -> {
                    assertEquals(accNumber, saved.getAccountNumber());
                    assertEquals(new BigDecimal("30.00"), saved.getValue());
                    assertEquals(new BigDecimal("70.00"), saved.getBalance());
                    assertEquals(key, saved.getIdempotencyKey());
                })
                .verifyComplete();

        verify(movementRepo, times(1)).findFirstByIdempotencyKey(key);
        verify(accountRepo).findByNumber(accNumber);
        verify(movementRepo).save(any(Movement.class));
        verify(accountRepo).updateBalance(accNumber, new BigDecimal("70.00"));
        verify(tx).transactional(any(Mono.class));
    }

    @Test
    void create_whenKeyNotFound_andDuplicateKeyException_shouldRecoverByFetchingExisting() {
        String key = "idem-race";
        String accNumber = "ACC-9";
        LocalDate date = LocalDate.now();

        Account acc = Account.builder()
                .number(accNumber)
                .balance(new BigDecimal("100.00"))
                .state(true)
                .build();

        Movement requested = Movement.builder()
                .accountNumber(accNumber)
                .date(date)
                .type(MovementType.DEPOSIT.apiValue())
                .value(new BigDecimal("5.00"))
                .detail("race")
                .build();

        Movement existing = Movement.builder()
                .id(UUID.randomUUID())
                .accountNumber(accNumber)
                .date(date)
                .type(MovementType.DEPOSIT.apiValue())
                .value(new BigDecimal("5.00"))
                .detail("race")
                .idempotencyKey(key)
                .build();

        when(movementRepo.findFirstByIdempotencyKey(key))
                .thenReturn(Mono.empty())
                .thenReturn(Mono.just(existing));

        when(accountRepo.findByNumber(accNumber)).thenReturn(Mono.just(acc));
        when(movementRepo.save(any(Movement.class))).thenReturn(Mono.error(new DuplicateKeyException("dup")));

        StepVerifier.create(service.create(requested, key))
                .expectNext(existing)
                .verifyComplete();

        verify(movementRepo, times(2)).findFirstByIdempotencyKey(key);
        verify(accountRepo).findByNumber(accNumber);
        verify(movementRepo).save(any(Movement.class));
        verify(tx).transactional(any(Mono.class));
        verify(accountRepo, never()).updateBalance(anyString(), any());
    }


    @Test
    void update_whenMovementIdNull_shouldThrowImmediately() {
        assertThrows(NullPointerException.class, () -> service.update(null, Movement.builder().build()));
        verifyNoInteractions(accountRepo, movementRepo);
    }

    @Test
    void update_whenPatchNull_shouldKeepDetailFromActual() {
        UUID id = UUID.randomUUID();

        Movement actual = Movement.builder()
                .id(id)
                .detail("ACTUAL")
                .accountNumber("ACC-10")
                .date(LocalDate.now())
                .type(MovementType.DEPOSIT.apiValue())
                .value(new BigDecimal("1.00"))
                .balance(new BigDecimal("10.00"))
                .build();

        when(movementRepo.findById(id)).thenReturn(Mono.just(actual));
        when(movementRepo.save(any(Movement.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(service.update(id, null))
                .assertNext(updated -> {
                    assertEquals(id, updated.getId());
                    assertEquals("ACTUAL", updated.getDetail());
                })
                .verifyComplete();

        verify(movementRepo).findById(id);
        verify(movementRepo).save(any(Movement.class));
    }

    @Test
    void update_whenPatchProvided_shouldUpdateDetailOnly() {
        UUID id = UUID.randomUUID();

        Movement actual = Movement.builder()
                .id(id)
                .detail("OLD")
                .accountNumber("ACC-11")
                .date(LocalDate.now())
                .type(MovementType.DEPOSIT.apiValue())
                .value(new BigDecimal("1.00"))
                .balance(new BigDecimal("10.00"))
                .build();

        Movement patch = Movement.builder()
                .detail("NEW")
                .build();

        when(movementRepo.findById(id)).thenReturn(Mono.just(actual));
        when(movementRepo.save(any(Movement.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(service.update(id, patch))
                .assertNext(updated -> {
                    assertEquals(id, updated.getId());
                    assertEquals("NEW", updated.getDetail());
                })
                .verifyComplete();

        ArgumentCaptor<Movement> captor = ArgumentCaptor.forClass(Movement.class);
        verify(movementRepo).save(captor.capture());
        assertEquals("NEW", captor.getValue().getDetail());
    }

    @Test
    void update_whenNotFound_shouldError() {
        UUID id = UUID.randomUUID();
        when(movementRepo.findById(id)).thenReturn(Mono.empty());

        StepVerifier.create(service.update(id, Movement.builder().detail("X").build()))
                .expectError(NotFoundException.class)
                .verify();

        verify(movementRepo).findById(id);
        verify(movementRepo, never()).save(any());
    }

    @Test
    void delete_whenMovementIdNull_shouldThrowImmediately() {
        assertThrows(NullPointerException.class, () -> service.delete(null));
        verifyNoInteractions(accountRepo, movementRepo);
    }

    @Test
    void delete_whenFound_shouldDeleteById() {
        UUID id = UUID.randomUUID();
        Movement found = Movement.builder().id(id).build();

        when(movementRepo.findById(id)).thenReturn(Mono.just(found));
        when(movementRepo.deleteById(id)).thenReturn(Mono.empty());

        StepVerifier.create(service.delete(id))
                .verifyComplete();

        verify(movementRepo).findById(id);
        verify(movementRepo).deleteById(id);
    }

    @Test
    void delete_whenNotFound_shouldError() {
        UUID id = UUID.randomUUID();
        when(movementRepo.findById(id)).thenReturn(Mono.empty());

        StepVerifier.create(service.delete(id))
                .expectError(NotFoundException.class)
                .verify();

        verify(movementRepo).findById(id);
        verify(movementRepo, never()).deleteById(any());
    }

    @Test
    void listByAccountBetween_whenAccountBlank_shouldErrorInvalidRequest() {
        StepVerifier.create(service.listByAccountBetween("  ", LocalDate.now(), LocalDate.now()))
                .expectError(InvalidRequestException.class)
                .verify();

        verifyNoInteractions(movementRepo, accountRepo);
    }

    @Test
    void listByAccountBetween_whenDatesMissing_shouldErrorInvalidRequest() {
        StepVerifier.create(service.listByAccountBetween("ACC-1", null, LocalDate.now()))
                .expectError(InvalidRequestException.class)
                .verify();

        StepVerifier.create(service.listByAccountBetween("ACC-1", LocalDate.now(), null))
                .expectError(InvalidRequestException.class)
                .verify();

        verifyNoInteractions(movementRepo, accountRepo);
    }

    @Test
    void listByAccountBetween_whenValid_shouldDelegateToRepo() {
        String accNumber = "ACC-12";
        LocalDate from = LocalDate.of(2025, 1, 1);
        LocalDate to = LocalDate.of(2025, 1, 31);

        Movement m1 = Movement.builder().id(UUID.randomUUID()).build();

        when(movementRepo.findByAccountAndDateRange(accNumber, from, to)).thenReturn(Flux.just(m1));

        StepVerifier.create(service.listByAccountBetween(accNumber, from, to))
                .expectNext(m1)
                .verifyComplete();

        verify(movementRepo).findByAccountAndDateRange(accNumber, from, to);
        verifyNoInteractions(accountRepo);
    }

    @Test
    void listPage_whenNoAccount_shouldUseCountAllAndFindPage_withDefaultClamp() {
        Movement m1 = Movement.builder().id(UUID.randomUUID()).build();

        when(movementRepo.countAll()).thenReturn(Mono.just(10L));
        when(movementRepo.findPage(eq(0), eq(50))).thenReturn(Flux.just(m1));

        StepVerifier.create(service.listPage(null, null, null, -1, 0))
                .assertNext(pr -> {
                    assertEquals(10L, pr.total());
                    StepVerifier.create(pr.items())
                            .expectNext(m1)
                            .verifyComplete();
                })
                .verifyComplete();

        verify(movementRepo).countAll();
        verify(movementRepo).findPage(0, 50);
        verifyNoInteractions(accountRepo);
    }

    @Test
    void listPage_whenAccountAndFromTo_shouldUseDateRangeBranch() {
        String accNumber = "ACC-13";
        LocalDate from = LocalDate.of(2025, 2, 1);
        LocalDate to = LocalDate.of(2025, 2, 10);

        Movement m1 = Movement.builder().id(UUID.randomUUID()).build();

        when(movementRepo.countByAccountAndDateRange(accNumber, from, to)).thenReturn(Mono.just(1L));
        when(movementRepo.findPageByAccountAndDateRange(eq(accNumber), eq(from), eq(to), eq(0), eq(50)))
                .thenReturn(Flux.just(m1));

        StepVerifier.create(service.listPage(accNumber, from, to, 0, 50))
                .assertNext(pr -> {
                    assertEquals(1L, pr.total());
                    StepVerifier.create(pr.items())
                            .expectNext(m1)
                            .verifyComplete();
                })
                .verifyComplete();

        verify(movementRepo).countByAccountAndDateRange(accNumber, from, to);
        verify(movementRepo).findPageByAccountAndDateRange(accNumber, from, to, 0, 50);
        verifyNoInteractions(accountRepo);
    }

    @Test
    void listPage_whenAccountOnly_shouldUseAccountBranch() {
        String accNumber = "ACC-14";
        Movement m1 = Movement.builder().id(UUID.randomUUID()).build();

        when(movementRepo.countByAccount(accNumber)).thenReturn(Mono.just(2L));
        when(movementRepo.findPageByAccount(eq(accNumber), eq(200), eq(200))).thenReturn(Flux.just(m1));

        StepVerifier.create(service.listPage(accNumber, null, null, 1, 999))
                .assertNext(pr -> {
                    assertEquals(2L, pr.total());
                    StepVerifier.create(pr.items())
                            .expectNext(m1)
                            .verifyComplete();
                })
                .verifyComplete();

        verify(movementRepo).countByAccount(accNumber);
        verify(movementRepo).findPageByAccount(accNumber, 200, 200);
        verifyNoInteractions(accountRepo);
    }

    @Test
    void listPage_whenOnlyOneDateProvided_shouldErrorInvalidRequest() {
        StepVerifier.create(service.listPage("ACC-15", LocalDate.now(), null, 0, 50))
                .expectError(InvalidRequestException.class)
                .verify();

        StepVerifier.create(service.listPage("ACC-15", null, LocalDate.now(), 0, 50))
                .expectError(InvalidRequestException.class)
                .verify();

        verifyNoInteractions(accountRepo, movementRepo);
    }
}
