package com.business.banking.services.infrastructure.output.repository.impl;

import com.business.banking.services.domain.model.Account;
import com.business.banking.services.infrastructure.output.repository.entity.AccountEntity;
import com.business.banking.services.infrastructure.output.repository.reactive.AccountR2dbcRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class AccountRepositoryImplTest {

    @Mock
    private AccountR2dbcRepository repo;

    @InjectMocks
    private AccountRepositoryImpl repository;

    private static AccountEntity entity(String number, UUID customerId) {
        return AccountEntity.builder()
                .number(number)
                .type("Ahorro")
                .balance(new BigDecimal("100.00"))
                .state(true)
                .customerId(customerId)
                .isNew(false)
                .build();
    }

    private static Account domain(String number, UUID customerId) {
        return Account.builder()
                .number(number)
                .type("Ahorro")
                .balance(new BigDecimal("100.00"))
                .state(true)
                .customerId(customerId)
                .build();
    }

    @Test
    void findAll_shouldReturnMappedDomainAccounts() {
        UUID customerId = UUID.randomUUID();

        when(repo.findAll()).thenReturn(Flux.just(
                entity("001", customerId),
                entity("002", customerId)
        ));

        StepVerifier.create(repository.findAll())
                .assertNext(a -> {
                    assertEquals("001", a.getNumber());
                    assertEquals("Ahorro", a.getType());
                    assertEquals(new BigDecimal("100.00"), a.getBalance());
                    assertTrue(a.getState());
                    assertEquals(customerId, a.getCustomerId());
                })
                .assertNext(a -> assertEquals("002", a.getNumber()))
                .verifyComplete();

        verify(repo).findAll();
        verifyNoMoreInteractions(repo);
    }


    @Test
    void findByNumber_shouldReturnMappedAccount() {
        UUID customerId = UUID.randomUUID();

        when(repo.findById("ABC")).thenReturn(Mono.just(entity("ABC", customerId)));

        StepVerifier.create(repository.findByNumber("ABC"))
                .assertNext(a -> {
                    assertEquals("ABC", a.getNumber());
                    assertEquals(customerId, a.getCustomerId());
                })
                .verifyComplete();

        verify(repo).findById("ABC");
        verifyNoMoreInteractions(repo);
    }

    @Test
    void findByNumber_whenNotFound_shouldReturnEmpty() {
        when(repo.findById("NOT_FOUND")).thenReturn(Mono.empty());

        StepVerifier.create(repository.findByNumber("NOT_FOUND"))
                .verifyComplete();

        verify(repo).findById("NOT_FOUND");
        verifyNoMoreInteractions(repo);
    }

    @Test
    void findByCustomerId_shouldReturnMappedAccounts() {
        UUID customerId = UUID.randomUUID();

        when(repo.findAllByCustomerId(customerId)).thenReturn(Flux.just(
                entity("001", customerId),
                entity("002", customerId)
        ));

        StepVerifier.create(repository.findByCustomerId(customerId))
                .expectNextMatches(a -> "001".equals(a.getNumber()))
                .expectNextMatches(a -> "002".equals(a.getNumber()))
                .verifyComplete();

        verify(repo).findAllByCustomerId(customerId);
        verifyNoMoreInteractions(repo);
    }

    @Test
    void existsByNumber_shouldDelegateToRepo() {
        when(repo.existsByNumber("XYZ")).thenReturn(Mono.just(true));

        StepVerifier.create(repository.existsByNumber("XYZ"))
                .expectNext(true)
                .verifyComplete();

        verify(repo).existsByNumber("XYZ");
        verifyNoMoreInteractions(repo);
    }

    @Test
    void save_whenAccountDoesNotExist_shouldSaveWithIsNewTrue_andReturnMappedDomain() {
        UUID customerId = UUID.randomUUID();

        Account input = domain("ACC-1", customerId);

        when(repo.existsById("ACC-1")).thenReturn(Mono.just(false));
        ArgumentCaptor<AccountEntity> captor = ArgumentCaptor.forClass(AccountEntity.class);

        when(repo.save(any(AccountEntity.class)))
                .thenReturn(Mono.just(
                        AccountEntity.builder()
                                .number("ACC-1")
                                .type("Ahorro")
                                .balance(new BigDecimal("100.00"))
                                .state(true)
                                .customerId(customerId)
                                .isNew(true)
                                .build()
                ));

        StepVerifier.create(repository.save(input))
                .assertNext(saved -> {
                    assertEquals("ACC-1", saved.getNumber());
                    assertEquals("Ahorro", saved.getType());
                    assertEquals(new BigDecimal("100.00"), saved.getBalance());
                    assertTrue(saved.getState());
                    assertEquals(customerId, saved.getCustomerId());
                })
                .verifyComplete();

        verify(repo).existsById("ACC-1");
        verify(repo).save(captor.capture());
        verifyNoMoreInteractions(repo);

        AccountEntity toSave = captor.getValue();
        assertNotNull(toSave);
        assertEquals("ACC-1", toSave.getNumber());
        assertEquals("Ahorro", toSave.getType());
        assertEquals(new BigDecimal("100.00"), toSave.getBalance());
        assertTrue(toSave.getState());
        assertEquals(customerId, toSave.getCustomerId());

        assertTrue(toSave.isNew());
    }

    @Test
    void save_whenAccountAlreadyExists_shouldSaveWithIsNewFalse_andReturnMappedDomain() {
        UUID customerId = UUID.randomUUID();

        Account input = domain("ACC-2", customerId);

        when(repo.existsById("ACC-2")).thenReturn(Mono.just(true));

        ArgumentCaptor<AccountEntity> captor = ArgumentCaptor.forClass(AccountEntity.class);

        when(repo.save(any(AccountEntity.class)))
                .thenReturn(Mono.just(
                        AccountEntity.builder()
                                .number("ACC-2")
                                .type("Ahorro")
                                .balance(new BigDecimal("100.00"))
                                .state(true)
                                .customerId(customerId)
                                .isNew(false)
                                .build()
                ));

        StepVerifier.create(repository.save(input))
                .assertNext(saved -> assertEquals("ACC-2", saved.getNumber()))
                .verifyComplete();

        verify(repo).existsById("ACC-2");
        verify(repo).save(captor.capture());
        verifyNoMoreInteractions(repo);
        assertFalse(captor.getValue().isNew());
    }

    @Test
    void deleteByNumber_shouldDelegateToRepo() {
        when(repo.deleteById("DEL-1")).thenReturn(Mono.empty());

        StepVerifier.create(repository.deleteByNumber("DEL-1"))
                .verifyComplete();

        verify(repo).deleteById("DEL-1");
        verifyNoMoreInteractions(repo);
    }


    @Test
    void updateBalance_shouldDelegateToRepo() {
        when(repo.updateBalance("ACC-9", new BigDecimal("999.99")))
                .thenReturn(Mono.just(1));

        StepVerifier.create(repository.updateBalance("ACC-9", new BigDecimal("999.99")))
                .expectNext(1)
                .verifyComplete();

        verify(repo).updateBalance("ACC-9", new BigDecimal("999.99"));
        verifyNoMoreInteractions(repo);
    }

    @Test
    void findPage_shouldReturnMappedAccounts() {
        UUID customerId = UUID.randomUUID();

        when(repo.findPage(0, 2)).thenReturn(Flux.just(
                entity("P1", customerId),
                entity("P2", customerId)
        ));

        StepVerifier.create(repository.findPage(0, 2))
                .expectNextMatches(a -> "P1".equals(a.getNumber()))
                .expectNextMatches(a -> "P2".equals(a.getNumber()))
                .verifyComplete();

        verify(repo).findPage(0, 2);
        verifyNoMoreInteractions(repo);
    }

    @Test
    void findPageByCustomerId_shouldReturnMappedAccounts() {
        UUID customerId = UUID.randomUUID();

        when(repo.findPageByCustomerId(customerId, 5, 10)).thenReturn(Flux.just(
                entity("C1", customerId),
                entity("C2", customerId)
        ));

        StepVerifier.create(repository.findPageByCustomerId(customerId, 5, 10))
                .expectNextMatches(a -> "C1".equals(a.getNumber()))
                .expectNextMatches(a -> "C2".equals(a.getNumber()))
                .verifyComplete();

        verify(repo).findPageByCustomerId(customerId, 5, 10);
        verifyNoMoreInteractions(repo);
    }

    @Test
    void countAll_shouldDelegateToRepo() {
        when(repo.countAll()).thenReturn(Mono.just(99L));

        StepVerifier.create(repository.countAll())
                .expectNext(99L)
                .verifyComplete();

        verify(repo).countAll();
        verifyNoMoreInteractions(repo);
    }

    @Test
    void countByCustomerId_shouldDelegateToRepo() {
        UUID customerId = UUID.randomUUID();

        when(repo.countByCustomerId(customerId)).thenReturn(Mono.just(10L));

        StepVerifier.create(repository.countByCustomerId(customerId))
                .expectNext(10L)
                .verifyComplete();

        verify(repo).countByCustomerId(customerId);
        verifyNoMoreInteractions(repo);
    }
}
