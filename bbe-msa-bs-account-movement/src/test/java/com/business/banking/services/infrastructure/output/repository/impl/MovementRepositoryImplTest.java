package com.business.banking.services.infrastructure.output.repository.impl;

import com.business.banking.services.domain.model.Movement;
import com.business.banking.services.infrastructure.output.repository.entity.MovementEntity;
import com.business.banking.services.infrastructure.output.repository.reactive.MovementR2dbcRepository;
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
class MovementRepositoryImplTest {

    @Mock
    private MovementR2dbcRepository repo;

    @InjectMocks
    private MovementRepositoryImpl repository;

    private static MovementEntity entity(UUID id, String accountNumber) {
        return MovementEntity.builder()
                .id(id)
                .accountNumber(accountNumber)
                .date(LocalDate.of(2025, 1, 10))
                .type("Deposito")
                .value(new BigDecimal("10.00"))
                .balance(new BigDecimal("100.00"))
                .detail("Payroll")
                .idempotencyKey("idem-1")
                .build();
    }

    private static Movement domain(UUID id, String accountNumber) {
        return Movement.builder()
                .id(id)
                .accountNumber(accountNumber)
                .date(LocalDate.of(2025, 1, 10))
                .type("Deposito")
                .value(new BigDecimal("10.00"))
                .balance(new BigDecimal("100.00"))
                .detail("Payroll")
                .idempotencyKey("idem-1")
                .build();
    }

    @Test
    void findAll_shouldReturnMappedMovements() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        when(repo.findAll()).thenReturn(Flux.just(
                entity(id1, "ACC-1"),
                entity(id2, "ACC-2")
        ));

        StepVerifier.create(repository.findAll())
                .assertNext(m -> {
                    assertEquals(id1, m.getId());
                    assertEquals("ACC-1", m.getAccountNumber());
                    assertEquals(LocalDate.of(2025, 1, 10), m.getDate());
                    assertEquals("Deposito", m.getType());
                    assertEquals(new BigDecimal("10.00"), m.getValue());
                    assertEquals(new BigDecimal("100.00"), m.getBalance());
                    assertEquals("Payroll", m.getDetail());
                    assertEquals("idem-1", m.getIdempotencyKey());
                })
                .assertNext(m -> assertEquals(id2, m.getId()))
                .verifyComplete();

        verify(repo).findAll();
        verifyNoMoreInteractions(repo);
    }

    @Test
    void findById_shouldReturnMappedMovement() {
        UUID id = UUID.randomUUID();

        when(repo.findById(id)).thenReturn(Mono.just(entity(id, "ACC-9")));

        StepVerifier.create(repository.findById(id))
                .assertNext(m -> {
                    assertEquals(id, m.getId());
                    assertEquals("ACC-9", m.getAccountNumber());
                    assertEquals("Payroll", m.getDetail());
                })
                .verifyComplete();

        verify(repo).findById(id);
        verifyNoMoreInteractions(repo);
    }

    @Test
    void findById_whenNotFound_shouldReturnEmpty() {
        UUID id = UUID.randomUUID();

        when(repo.findById(id)).thenReturn(Mono.empty());

        StepVerifier.create(repository.findById(id))
                .verifyComplete();

        verify(repo).findById(id);
        verifyNoMoreInteractions(repo);
    }

    @Test
    void save_shouldMapToEntity_callRepo_andReturnMappedDomain() {
        UUID id = UUID.randomUUID();
        Movement input = domain(id, "ACC-1");

        ArgumentCaptor<MovementEntity> captor = ArgumentCaptor.forClass(MovementEntity.class);

        when(repo.save(any(MovementEntity.class)))
                .thenReturn(Mono.just(entity(id, "ACC-1")));

        StepVerifier.create(repository.save(input))
                .assertNext(saved -> {
                    assertEquals(id, saved.getId());
                    assertEquals("ACC-1", saved.getAccountNumber());
                    assertEquals("Payroll", saved.getDetail());
                    assertEquals("idem-1", saved.getIdempotencyKey());
                })
                .verifyComplete();

        verify(repo).save(captor.capture());
        verifyNoMoreInteractions(repo);

        MovementEntity toSave = captor.getValue();
        assertNotNull(toSave);
        assertEquals(id, toSave.getId());
        assertEquals("ACC-1", toSave.getAccountNumber());
        assertEquals(LocalDate.of(2025, 1, 10), toSave.getDate());
        assertEquals("Deposito", toSave.getType());
        assertEquals(new BigDecimal("10.00"), toSave.getValue());
        assertEquals(new BigDecimal("100.00"), toSave.getBalance());
        assertEquals("Payroll", toSave.getDetail());
        assertEquals("idem-1", toSave.getIdempotencyKey());
    }

    @Test
    void deleteById_shouldDelegateToRepo() {
        UUID id = UUID.randomUUID();

        when(repo.deleteById(id)).thenReturn(Mono.empty());

        StepVerifier.create(repository.deleteById(id))
                .verifyComplete();

        verify(repo).deleteById(id);
        verifyNoMoreInteractions(repo);
    }

    @Test
    void findByAccount_shouldReturnMappedMovementsOrderedAsc() {
        when(repo.findAllByAccountNumberOrderByDateAsc("ACC-1"))
                .thenReturn(Flux.just(
                        entity(UUID.randomUUID(), "ACC-1"),
                        entity(UUID.randomUUID(), "ACC-1")
                ));

        StepVerifier.create(repository.findByAccount("ACC-1"))
                .expectNextCount(2)
                .verifyComplete();

        verify(repo).findAllByAccountNumberOrderByDateAsc("ACC-1");
        verifyNoMoreInteractions(repo);
    }

    @Test
    void findByAccountAndDateRange_shouldReturnMappedMovements() {
        LocalDate from = LocalDate.of(2025, 1, 1);
        LocalDate to = LocalDate.of(2025, 1, 31);

        when(repo.findAllByAccountNumberAndDateBetweenOrderByDateAsc("ACC-1", from, to))
                .thenReturn(Flux.just(entity(UUID.randomUUID(), "ACC-1")));

        StepVerifier.create(repository.findByAccountAndDateRange("ACC-1", from, to))
                .assertNext(m -> assertEquals("ACC-1", m.getAccountNumber()))
                .verifyComplete();

        verify(repo).findAllByAccountNumberAndDateBetweenOrderByDateAsc("ACC-1", from, to);
        verifyNoMoreInteractions(repo);
    }

    @Test
    void findFirstByIdempotencyKey_shouldReturnMappedMovement() {
        when(repo.findFirstByIdempotencyKey("idem-x"))
                .thenReturn(Mono.just(entity(UUID.randomUUID(), "ACC-8")));

        StepVerifier.create(repository.findFirstByIdempotencyKey("idem-x"))
                .assertNext(m -> assertEquals("idem-1", m.getIdempotencyKey()))
                .verifyComplete();

        verify(repo).findFirstByIdempotencyKey("idem-x");
        verifyNoMoreInteractions(repo);
    }

    @Test
    void findFirstByIdempotencyKey_whenEmpty_shouldReturnEmpty() {
        when(repo.findFirstByIdempotencyKey("idem-none"))
                .thenReturn(Mono.empty());

        StepVerifier.create(repository.findFirstByIdempotencyKey("idem-none"))
                .verifyComplete();

        verify(repo).findFirstByIdempotencyKey("idem-none");
        verifyNoMoreInteractions(repo);
    }

    @Test
    void findPage_shouldReturnMappedMovements() {
        when(repo.findPage(0, 2))
                .thenReturn(Flux.just(
                        entity(UUID.randomUUID(), "ACC-1"),
                        entity(UUID.randomUUID(), "ACC-2")
                ));

        StepVerifier.create(repository.findPage(0, 2))
                .expectNextCount(2)
                .verifyComplete();

        verify(repo).findPage(0, 2);
        verifyNoMoreInteractions(repo);
    }

    @Test
    void findPageByAccount_shouldReturnMappedMovements() {
        when(repo.findPageByAccount("ACC-1", 10, 5))
                .thenReturn(Flux.just(
                        entity(UUID.randomUUID(), "ACC-1")
                ));

        StepVerifier.create(repository.findPageByAccount("ACC-1", 10, 5))
                .assertNext(m -> assertEquals("ACC-1", m.getAccountNumber()))
                .verifyComplete();

        verify(repo).findPageByAccount("ACC-1", 10, 5);
        verifyNoMoreInteractions(repo);
    }

    @Test
    void findPageByAccountAndDateRange_shouldReturnMappedMovements() {
        LocalDate from = LocalDate.of(2025, 1, 1);
        LocalDate to = LocalDate.of(2025, 1, 31);

        when(repo.findPageByAccountAndDateRange("ACC-1", from, to, 0, 50))
                .thenReturn(Flux.just(entity(UUID.randomUUID(), "ACC-1")));

        StepVerifier.create(repository.findPageByAccountAndDateRange("ACC-1", from, to, 0, 50))
                .assertNext(m -> assertEquals("ACC-1", m.getAccountNumber()))
                .verifyComplete();

        verify(repo).findPageByAccountAndDateRange("ACC-1", from, to, 0, 50);
        verifyNoMoreInteractions(repo);
    }

    @Test
    void countAll_shouldDelegateToRepo() {
        when(repo.countAll()).thenReturn(Mono.just(123L));

        StepVerifier.create(repository.countAll())
                .expectNext(123L)
                .verifyComplete();

        verify(repo).countAll();
        verifyNoMoreInteractions(repo);
    }

    @Test
    void countByAccount_shouldDelegateToRepo() {
        when(repo.countByAccount("ACC-7")).thenReturn(Mono.just(7L));

        StepVerifier.create(repository.countByAccount("ACC-7"))
                .expectNext(7L)
                .verifyComplete();

        verify(repo).countByAccount("ACC-7");
        verifyNoMoreInteractions(repo);
    }

    @Test
    void countByAccountAndDateRange_shouldDelegateToRepo() {
        LocalDate from = LocalDate.of(2025, 1, 1);
        LocalDate to = LocalDate.of(2025, 1, 31);

        when(repo.countByAccountAndDateRange("ACC-7", from, to)).thenReturn(Mono.just(3L));

        StepVerifier.create(repository.countByAccountAndDateRange("ACC-7", from, to))
                .expectNext(3L)
                .verifyComplete();

        verify(repo).countByAccountAndDateRange("ACC-7", from, to);
        verifyNoMoreInteractions(repo);
    }

    @Test
    void findByAccountNumbersAndDateRange_whenNullCollection_shouldReturnEmpty() {
        StepVerifier.create(repository.findByAccountNumbersAndDateRange(null,
                        LocalDate.of(2025, 1, 1),
                        LocalDate.of(2025, 1, 31)))
                .verifyComplete();

        verifyNoInteractions(repo);
    }

    @Test
    void findByAccountNumbersAndDateRange_whenEmptyCollection_shouldReturnEmpty() {
        StepVerifier.create(repository.findByAccountNumbersAndDateRange(List.of(),
                        LocalDate.of(2025, 1, 1),
                        LocalDate.of(2025, 1, 31)))
                .verifyComplete();

        verifyNoInteractions(repo);
    }

    @Test
    void findByAccountNumbersAndDateRange_shouldDelegateToRepo_andReturnMappedMovements() {
        LocalDate from = LocalDate.of(2025, 1, 1);
        LocalDate to = LocalDate.of(2025, 1, 31);

        List<String> accounts = List.of("ACC-1", "ACC-2");

        when(repo.findAllByAccountNumbersAndDateRange(accounts, from, to))
                .thenReturn(Flux.just(
                        entity(UUID.randomUUID(), "ACC-1"),
                        entity(UUID.randomUUID(), "ACC-2")
                ));

        StepVerifier.create(repository.findByAccountNumbersAndDateRange(accounts, from, to))
                .expectNextCount(2)
                .verifyComplete();

        verify(repo).findAllByAccountNumbersAndDateRange(accounts, from, to);
        verifyNoMoreInteractions(repo);
    }
}
