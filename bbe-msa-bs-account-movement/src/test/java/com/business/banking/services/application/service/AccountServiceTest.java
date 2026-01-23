package com.business.banking.services.application.service;

import com.business.banking.services.application.output.port.AccountRepositoryPort;
import com.business.banking.services.application.shared.PageResult;
import com.business.banking.services.domain.exception.DuplicateAccountNumberException;
import com.business.banking.services.domain.exception.NotFoundException;
import com.business.banking.services.domain.model.Account;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepositoryPort repo;

    @InjectMocks
    private AccountService service;

    private UUID customerId;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
    }

    @Test
    void list_whenCustomerIdProvided_shouldCallFindByCustomerId() {
        var a1 = Account.builder().number("001").build();
        var a2 = Account.builder().number("002").build();

        when(repo.findByCustomerId(customerId)).thenReturn(Flux.just(a1, a2));

        StepVerifier.create(service.list(customerId))
                .expectNext(a1)
                .expectNext(a2)
                .verifyComplete();

        verify(repo).findByCustomerId(customerId);
        verify(repo, never()).findAll();
    }

    @Test
    void list_whenCustomerIdNull_shouldCallFindAll() {
        var a1 = Account.builder().number("001").build();

        when(repo.findAll()).thenReturn(Flux.just(a1));

        StepVerifier.create(service.list(null))
                .expectNext(a1)
                .verifyComplete();

        verify(repo).findAll();
        verify(repo, never()).findByCustomerId(any());
    }

    @Test
    void listPage_whenCustomerIdNull_shouldUseCountAll_andFindPage_withClampedDefaults() {
        var a1 = Account.builder().number("001").build();
        when(repo.countAll()).thenReturn(Mono.just(10L));
        when(repo.findPage(eq(0), eq(50))).thenReturn(Flux.just(a1));

        Mono<PageResult<Account>> mono = service.listPage(null, -5, 0);

        StepVerifier.create(mono)
                .assertNext(pr -> {
                    org.junit.jupiter.api.Assertions.assertEquals(10L, pr.total());
                    StepVerifier.create(pr.items())
                            .expectNext(a1)
                            .verifyComplete();
                })
                .verifyComplete();

        verify(repo).countAll();
        verify(repo).findPage(0, 50);
        verify(repo, never()).countByCustomerId(any());
        verify(repo, never()).findPageByCustomerId(any(), anyInt(), anyInt());
    }


    @Test
    void listPage_whenCustomerIdProvided_shouldUseCountByCustomerId_andFindPageByCustomerId_withClampMax() {
        var a1 = Account.builder().number("001").build();

        when(repo.countByCustomerId(customerId)).thenReturn(Mono.just(1L));
        when(repo.findPageByCustomerId(eq(customerId), eq(200), eq(200))).thenReturn(Flux.just(a1));

        Mono<PageResult<Account>> mono = service.listPage(customerId, 1, 999);

        StepVerifier.create(mono)
                .assertNext(pr -> {
                    org.junit.jupiter.api.Assertions.assertEquals(1L, pr.total());
                    StepVerifier.create(pr.items())
                            .expectNext(a1)
                            .verifyComplete();
                })
                .verifyComplete();

        verify(repo).countByCustomerId(customerId);
        verify(repo).findPageByCustomerId(customerId, 200, 200);
        verify(repo, never()).countAll();
        verify(repo, never()).findPage(anyInt(), anyInt());
    }


    @Test
    void getByNumber_whenFound_shouldReturnAccount() {
        var acc = Account.builder().number("ABC").build();
        when(repo.findByNumber("ABC")).thenReturn(Mono.just(acc));

        StepVerifier.create(service.getByNumber("ABC"))
                .expectNext(acc)
                .verifyComplete();

        verify(repo).findByNumber("ABC");
    }

    @Test
    void getByNumber_whenNotFound_shouldErrorNotFound() {
        when(repo.findByNumber("ABC")).thenReturn(Mono.empty());

        StepVerifier.create(service.getByNumber("ABC"))
                .expectError(NotFoundException.class)
                .verify();

        verify(repo).findByNumber("ABC");
    }

    @Test
    void create_whenAccountNull_shouldThrowImmediately() {
        assertThrows(NullPointerException.class, () -> service.create(null));
        verifyNoInteractions(repo);
    }

    @Test
    void create_whenNumberAlreadyExists_shouldErrorDuplicate() {
        var acc = Account.builder().number("DUP").build();
        when(repo.existsByNumber("DUP")).thenReturn(Mono.just(true));

        StepVerifier.create(service.create(acc))
                .expectError(DuplicateAccountNumberException.class)
                .verify();

        verify(repo).existsByNumber("DUP");
        verify(repo, never()).save(any());
    }

    @Test
    void create_whenNumberDoesNotExist_shouldSave() {
        var acc = Account.builder().number("NEW").build();
        var saved = Account.builder().number("NEW").build();

        when(repo.existsByNumber("NEW")).thenReturn(Mono.just(false));
        when(repo.save(acc)).thenReturn(Mono.just(saved));

        StepVerifier.create(service.create(acc))
                .expectNext(saved)
                .verifyComplete();

        verify(repo).existsByNumber("NEW");
        verify(repo).save(acc);
    }

    @Test
    void update_whenPatchNull_shouldKeepActualValues() {
        var actual = Account.builder()
                .number("ACC-1")
                .type("SAVINGS")
                .balance(new BigDecimal("100.00"))
                .state(true)
                .customerId(customerId)
                .build();

        when(repo.findByNumber("ACC-1")).thenReturn(Mono.just(actual));
        when(repo.save(any(Account.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        Account emptyPatch = Account.builder()
                .type(null)
                .balance(null)
                .state(null)
                .customerId(null)
                .build();

        StepVerifier.create(service.update("ACC-1", emptyPatch))
                .assertNext(updated -> {
                    org.junit.jupiter.api.Assertions.assertEquals("ACC-1", updated.getNumber());
                    org.junit.jupiter.api.Assertions.assertEquals("SAVINGS", updated.getType());
                    org.junit.jupiter.api.Assertions.assertEquals(new BigDecimal("100.00"), updated.getBalance());
                    org.junit.jupiter.api.Assertions.assertTrue(updated.getState());
                    org.junit.jupiter.api.Assertions.assertEquals(customerId, updated.getCustomerId());
                })
                .verifyComplete();

        verify(repo).findByNumber("ACC-1");

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        verify(repo).save(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals("ACC-1", captor.getValue().getNumber());
        org.junit.jupiter.api.Assertions.assertEquals(new BigDecimal("100.00"), captor.getValue().getBalance());
    }


    @Test
    void update_whenPatchProvided_shouldMergeNonNullFields() {
        var actual = Account.builder()
                .number("ACC-2")
                .type("SAVINGS")
                .balance(new BigDecimal("100.00"))
                .state(true)
                .customerId(customerId)
                .build();

        var patch = Account.builder()
                .type("CURRENT")
                .balance(new BigDecimal("250.50"))
                .state(false)
                .build();

        when(repo.findByNumber("ACC-2")).thenReturn(Mono.just(actual));
        when(repo.save(any(Account.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(service.update("ACC-2", patch))
                .assertNext(updated -> {
                    org.junit.jupiter.api.Assertions.assertEquals("ACC-2", updated.getNumber());
                    org.junit.jupiter.api.Assertions.assertEquals("CURRENT", updated.getType());
                    org.junit.jupiter.api.Assertions.assertEquals(new BigDecimal("250.50"), updated.getBalance());
                    org.junit.jupiter.api.Assertions.assertFalse(updated.getState());
                    org.junit.jupiter.api.Assertions.assertEquals(customerId, updated.getCustomerId());
                })
                .verifyComplete();

        verify(repo).findByNumber("ACC-2");
        verify(repo).save(any(Account.class));
    }

    @Test
    void update_whenNotFound_shouldError() {
        when(repo.findByNumber("MISSING")).thenReturn(Mono.empty());

        StepVerifier.create(service.update("MISSING", Account.builder().type("X").build()))
                .expectError(NotFoundException.class)
                .verify();

        verify(repo).findByNumber("MISSING");
        verify(repo, never()).save(any());
    }

    @Test
    void delete_whenFound_shouldDeleteByNumber() {
        var acc = Account.builder().number("DEL").build();
        when(repo.findByNumber("DEL")).thenReturn(Mono.just(acc));
        when(repo.deleteByNumber("DEL")).thenReturn(Mono.empty());

        StepVerifier.create(service.delete("DEL"))
                .verifyComplete();

        verify(repo).findByNumber("DEL");
        verify(repo).deleteByNumber("DEL");
    }

    @Test
    void delete_whenNotFound_shouldError() {
        when(repo.findByNumber("DEL")).thenReturn(Mono.empty());

        StepVerifier.create(service.delete("DEL"))
                .expectError(NotFoundException.class)
                .verify();

        verify(repo).findByNumber("DEL");
        verify(repo, never()).deleteByNumber(anyString());
    }
}
