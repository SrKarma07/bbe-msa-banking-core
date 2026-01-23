package com.business.banking.services.application.service;

import com.business.banking.services.application.output.port.CustomerRepositoryPort;
import com.business.banking.services.application.shared.PageResult;
import com.business.banking.services.domain.exception.DuplicateIdentificationException;
import com.business.banking.services.domain.exception.InvalidRequestException;
import com.business.banking.services.domain.exception.NotFoundException;
import com.business.banking.services.domain.model.Customer;
import com.business.banking.services.domain.model.Person;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    CustomerRepositoryPort repo;

    @Mock
    PasswordEncoder passwordEncoder;

    @InjectMocks
    CustomerService service;

    private static final UUID ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private static Person validPersonJose() {
        return Person.builder()
                .name("Jose Lema")
                .gender("M")
                .age(30)
                .identification("098254785")
                .address("Otavalo sn y principal")
                .phoneNumber("098254785")
                .build();
    }

    private static Customer reqCustomer(Person p, String password, Boolean state, UUID id) {
        return Customer.builder()
                .id(id)
                .person(p)
                .password(password)
                .state(state)
                .build();
    }

    private static Customer actualCustomerJose() {
        return Customer.builder()
                .id(ID)
                .person(validPersonJose())
                .password("ENC_ACTUAL")
                .state(true)
                .build();
    }

    // ---------------- list ----------------

    @Test
    void list_shouldReturnAll() {
        Customer c1 = actualCustomerJose();
        Customer c2 = Customer.builder()
                .id(UUID.randomUUID())
                .person(validPersonJose())
                .password("X")
                .state(true)
                .build();

        when(repo.findAll()).thenReturn(Flux.just(c1, c2));

        StepVerifier.create(service.list())
                .expectNext(c1)
                .expectNext(c2)
                .verifyComplete();

        verify(repo).findAll();
        verifyNoMoreInteractions(repo);
        verifyNoInteractions(passwordEncoder);
    }

    // ---------------- listPage ----------------

    @Test
    void listPage_shouldUseSafeBoundsAndComputeTotals() {
        when(repo.count(true, "id", "name")).thenReturn(Mono.just(401L));

        Customer c1 = actualCustomerJose();
        when(repo.findPage(true, "id", "name", 0, 200)).thenReturn(Flux.just(c1));

        StepVerifier.create(service.listPage(true, "id", "name", -5, 999))
                .assertNext(pr -> {
                    assertEquals(0, pr.page());
                    assertEquals(200, pr.size());
                    assertEquals(401L, pr.totalItems());
                    assertEquals(3, pr.totalPages());

                    List<Customer> items = pr.items().collectList().block();
                    assertNotNull(items);
                    assertEquals(1, items.size());
                    assertEquals(c1, items.get(0));
                })
                .verifyComplete();

        verify(repo).count(true, "id", "name");
        verify(repo).findPage(true, "id", "name", 0, 200);
        verifyNoMoreInteractions(repo);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void listPage_whenTotalIsZero_shouldHaveTotalPagesZero() {
        when(repo.count(null, null, null)).thenReturn(Mono.just(0L));
        when(repo.findPage(null, null, null, 0, 1)).thenReturn(Flux.empty());

        StepVerifier.create(service.listPage(null, null, null, 0, 0))
                .assertNext(pr -> {
                    assertEquals(0, pr.page());
                    assertEquals(1, pr.size());
                    assertEquals(0L, pr.totalItems());
                    assertEquals(0, pr.totalPages());

                    List<?> items = pr.items().collectList().block();
                    assertNotNull(items);
                    assertTrue(items.isEmpty());
                })
                .verifyComplete();

        verify(repo).count(null, null, null);
        verify(repo).findPage(null, null, null, 0, 1);
        verifyNoMoreInteractions(repo);
        verifyNoInteractions(passwordEncoder);
    }

    // ---------------- getById ----------------

    @Test
    void getById_whenFound_shouldReturnCustomer() {
        Customer actual = actualCustomerJose();
        when(repo.findById(ID)).thenReturn(Mono.just(actual));

        StepVerifier.create(service.getById(ID))
                .expectNext(actual)
                .verifyComplete();

        verify(repo).findById(ID);
        verifyNoMoreInteractions(repo);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void getById_whenNotFound_shouldThrowNotFoundException() {
        when(repo.findById(ID)).thenReturn(Mono.empty());

        StepVerifier.create(service.getById(ID))
                .expectError(NotFoundException.class)
                .verify();

        verify(repo).findById(ID);
        verifyNoMoreInteractions(repo);
        verifyNoInteractions(passwordEncoder);
    }

    // ---------------- create (SYNC validations inside validatePersonForCreate) ----------------
    // IMPORTANT: these are thrown BEFORE returning Mono -> use assertThrows

    @Test
    void create_whenPersonNameMissing_shouldThrowSynchronously() {
        Person p = validPersonJose();
        p.setName("   ");

        assertThrows(InvalidRequestException.class,
                () -> service.create(reqCustomer(p, "12345678", true, ID)));

        verifyNoInteractions(repo);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void create_whenGenderMissing_shouldThrowSynchronously() {
        Person p = validPersonJose();
        p.setGender(" ");

        assertThrows(InvalidRequestException.class,
                () -> service.create(reqCustomer(p, "12345678", true, ID)));

        verifyNoInteractions(repo);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void create_whenGenderInvalidPattern_shouldThrowSynchronously() {
        Person p = validPersonJose();
        p.setGender("MALE");

        InvalidRequestException ex = assertThrows(InvalidRequestException.class,
                () -> service.create(reqCustomer(p, "12345678", true, ID)));

        assertTrue(ex.getMessage().contains("person.gender must match pattern"));

        verifyNoInteractions(repo);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void create_whenAgeMissing_shouldThrowSynchronously() {
        Person p = validPersonJose();
        p.setAge(null);

        assertThrows(InvalidRequestException.class,
                () -> service.create(reqCustomer(p, "12345678", true, ID)));

        verifyNoInteractions(repo);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void create_whenAgeOutOfRange_shouldThrowSynchronously() {
        Person p = validPersonJose();
        p.setAge(200);

        InvalidRequestException ex = assertThrows(InvalidRequestException.class,
                () -> service.create(reqCustomer(p, "12345678", true, ID)));

        assertTrue(ex.getMessage().contains("between 0 and 125"));

        verifyNoInteractions(repo);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void create_whenIdentificationMissing_shouldThrowSynchronously() {
        Person p = validPersonJose();
        p.setIdentification(" ");

        assertThrows(InvalidRequestException.class,
                () -> service.create(reqCustomer(p, "12345678", true, ID)));

        verifyNoInteractions(repo);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void create_whenIdentificationLengthInvalid_shouldThrowSynchronously() {
        Person p = validPersonJose();
        p.setIdentification("12");

        InvalidRequestException ex = assertThrows(InvalidRequestException.class,
                () -> service.create(reqCustomer(p, "12345678", true, ID)));

        assertTrue(ex.getMessage().contains("length must be between 3 and 50"));

        verifyNoInteractions(repo);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void create_whenAddressMissing_shouldThrowSynchronously() {
        Person p = validPersonJose();
        p.setAddress(" ");

        assertThrows(InvalidRequestException.class,
                () -> service.create(reqCustomer(p, "12345678", true, ID)));

        verifyNoInteractions(repo);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void create_whenPhoneMissing_shouldThrowSynchronously() {
        Person p = validPersonJose();
        p.setPhoneNumber("   ");

        assertThrows(InvalidRequestException.class,
                () -> service.create(reqCustomer(p, "12345678", true, ID)));

        verifyNoInteractions(repo);
        verifyNoInteractions(passwordEncoder);
    }

    // ---------------- create reactive validations ----------------

    @Test
    void create_whenBodyNull_shouldErrorReactive() {
        StepVerifier.create(service.create(null))
                .expectError(InvalidRequestException.class)
                .verify();

        verifyNoInteractions(repo);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void create_whenPersonMissing_shouldErrorReactive() {
        Customer c = Customer.builder().password("12345678").state(true).build();

        StepVerifier.create(service.create(c))
                .expectError(InvalidRequestException.class)
                .verify();

        verifyNoInteractions(repo);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void create_whenPasswordMissingOrBlank_shouldErrorReactive() {
        Customer c1 = reqCustomer(validPersonJose(), null, true, ID);
        Customer c2 = reqCustomer(validPersonJose(), "   ", true, ID);

        StepVerifier.create(service.create(c1))
                .expectError(InvalidRequestException.class)
                .verify();

        StepVerifier.create(service.create(c2))
                .expectError(InvalidRequestException.class)
                .verify();

        verifyNoInteractions(repo);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void create_whenPasswordTooShort_shouldErrorReactive() {
        Customer c = reqCustomer(validPersonJose(), "123", true, ID);

        StepVerifier.create(service.create(c))
                .expectError(InvalidRequestException.class)
                .verify();

        verifyNoInteractions(repo);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void create_whenStateMissing_shouldErrorReactive() {
        Customer c = reqCustomer(validPersonJose(), "12345678", null, ID);

        StepVerifier.create(service.create(c))
                .expectError(InvalidRequestException.class)
                .verify();

        verifyNoInteractions(repo);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void create_whenDuplicateIdentification_shouldErrorReactive() {
        Customer req = reqCustomer(validPersonJose(), "12345678", true, ID);

        when(passwordEncoder.encode("12345678")).thenReturn("ENC");
        when(repo.existsByIdentification(req.getPerson().getIdentification())).thenReturn(Mono.just(true));

        StepVerifier.create(service.create(req))
                .expectError(DuplicateIdentificationException.class)
                .verify();

        verify(passwordEncoder).encode("12345678");
        verify(repo).existsByIdentification(req.getPerson().getIdentification());
        verify(repo, never()).create(any());
        verifyNoMoreInteractions(repo);
    }

    @Test
    void create_whenOk_shouldCreateWithEncodedPassword_andKeepId() {
        Customer req = reqCustomer(validPersonJose(), "12345678", true, ID);

        when(passwordEncoder.encode("12345678")).thenReturn("ENC");
        when(repo.existsByIdentification(req.getPerson().getIdentification())).thenReturn(Mono.just(false));

        ArgumentCaptor<Customer> captor = ArgumentCaptor.forClass(Customer.class);
        when(repo.create(captor.capture())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(service.create(req))
                .assertNext(created -> {
                    assertEquals(ID, created.getId());
                    assertEquals("ENC", created.getPassword());
                    assertTrue(created.getState());
                    assertEquals("Jose Lema", created.getPerson().getName());
                })
                .verifyComplete();

        verify(passwordEncoder).encode("12345678");
        verify(repo).existsByIdentification(req.getPerson().getIdentification());
        verify(repo).create(any(Customer.class));
        verifyNoMoreInteractions(repo);
    }

    // ---------------- update ----------------

    @Test
    void update_whenPatchNull_shouldKeepActual_andCallUpdate() {
        Customer actual = actualCustomerJose();
        when(repo.findById(ID)).thenReturn(Mono.just(actual));
        when(repo.update(eq(ID), any(Customer.class))).thenAnswer(inv -> Mono.just(inv.getArgument(1)));

        StepVerifier.create(service.update(ID, null))
                .assertNext(updated -> {
                    assertEquals(ID, updated.getId());
                    assertEquals(actual.getPassword(), updated.getPassword());
                    assertEquals(actual.getState(), updated.getState());
                    assertEquals(actual.getPerson().getIdentification(), updated.getPerson().getIdentification());
                })
                .verifyComplete();

        verify(repo).findById(ID);
        verify(repo).update(eq(ID), any(Customer.class));
        verify(repo, never()).existsByIdentificationAndDifferentCustomer(any(), any());
        verifyNoMoreInteractions(repo);
        verifyNoInteractions(passwordEncoder);
    }

//    @Test
//    void update_whenIdentificationNotChangedBecauseBlank_shouldSkipGuard_andUpdate() {
//        Customer actual = actualCustomerJose();
//        when(repo.findById(ID)).thenReturn(Mono.just(actual));
//
//        // IMPORTANT: merge uses firstNonNull, not "blank check" for other fields.
//        // So we only assert what is guaranteed: identification does not change AND guard is not called.
//        Customer patch = Customer.builder()
//                .person(Person.builder().identification("   ").address("Nueva").build())
//                .build();
//
//        when(repo.update(eq(ID), any(Customer.class))).thenAnswer(inv -> Mono.just(inv.getArgument(1)));
//
//        StepVerifier.create(service.update(ID, patch))
//                .assertNext(updated -> {
//                    assertEquals(actual.getPerson().getIdentification(), updated.getPerson().getIdentification());
//                    assertEquals("Nueva", updated.getPerson().getAddress());
//                })
//                .verifyComplete();
//
//        verify(repo).findById(ID);
//        verify(repo, never()).existsByIdentificationAndDifferentCustomer(any(), any());
//        verify(repo).update(eq(ID), any(Customer.class));
//        verifyNoMoreInteractions(repo);
//        verifyNoInteractions(passwordEncoder);
//    }

    @Test
    void update_whenIdentificationChangedAndDuplicate_shouldError() {
        Customer actual = actualCustomerJose();
        when(repo.findById(ID)).thenReturn(Mono.just(actual));

        Customer patch = Customer.builder()
                .person(Person.builder().identification("DUPLICATE-ID").build())
                .build();

        when(repo.existsByIdentificationAndDifferentCustomer(ID, "DUPLICATE-ID"))
                .thenReturn(Mono.just(true));

        StepVerifier.create(service.update(ID, patch))
                .expectError(DuplicateIdentificationException.class)
                .verify();

        verify(repo).findById(ID);
        verify(repo).existsByIdentificationAndDifferentCustomer(ID, "DUPLICATE-ID");
        verify(repo, never()).update(any(), any());
        verifyNoMoreInteractions(repo);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void update_whenIdentificationChangedAndNotDuplicate_shouldMergeAndUpdate() {
        Customer actual = actualCustomerJose();
        when(repo.findById(ID)).thenReturn(Mono.just(actual));

        Customer patch = Customer.builder()
                .person(Person.builder()
                        .identification("NEW-ID")
                        .address("Nueva direccion")
                        .build())
                .state(false)
                .build();

        when(repo.existsByIdentificationAndDifferentCustomer(ID, "NEW-ID"))
                .thenReturn(Mono.just(false));

        when(repo.update(eq(ID), any(Customer.class))).thenAnswer(inv -> Mono.just(inv.getArgument(1)));

        StepVerifier.create(service.update(ID, patch))
                .assertNext(updated -> {
                    assertEquals("NEW-ID", updated.getPerson().getIdentification());
                    assertEquals("Nueva direccion", updated.getPerson().getAddress());
                    assertEquals("Jose Lema", updated.getPerson().getName());
                    assertEquals("M", updated.getPerson().getGender());
                    assertEquals(30, updated.getPerson().getAge());
                    assertEquals(actual.getPassword(), updated.getPassword());
                    assertFalse(updated.getState());
                })
                .verifyComplete();

        verify(repo).findById(ID);
        verify(repo).existsByIdentificationAndDifferentCustomer(ID, "NEW-ID");
        verify(repo).update(eq(ID), any(Customer.class));
        verifyNoMoreInteractions(repo);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void update_whenPasswordProvidedBlank_shouldError() {
        Customer actual = actualCustomerJose();
        when(repo.findById(ID)).thenReturn(Mono.just(actual));

        Customer patch = Customer.builder().password("   ").build();

        StepVerifier.create(service.update(ID, patch))
                .expectError(InvalidRequestException.class)
                .verify();

        verify(repo).findById(ID);
        verify(repo, never()).update(any(), any());
        verifyNoMoreInteractions(repo);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void update_whenPasswordTooShort_shouldError() {
        Customer actual = actualCustomerJose();
        when(repo.findById(ID)).thenReturn(Mono.just(actual));

        Customer patch = Customer.builder().password("123").build();

        StepVerifier.create(service.update(ID, patch))
                .expectError(InvalidRequestException.class)
                .verify();

        verify(repo).findById(ID);
        verify(repo, never()).update(any(), any());
        verifyNoMoreInteractions(repo);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void update_whenPasswordValid_shouldEncodeAndUpdate() {
        Customer actual = actualCustomerJose();
        when(repo.findById(ID)).thenReturn(Mono.just(actual));

        Customer patch = Customer.builder().password("12345678").build();

        when(passwordEncoder.encode("12345678")).thenReturn("ENC_NEW");
        when(repo.update(eq(ID), any(Customer.class))).thenAnswer(inv -> Mono.just(inv.getArgument(1)));

        StepVerifier.create(service.update(ID, patch))
                .assertNext(updated -> assertEquals("ENC_NEW", updated.getPassword()))
                .verifyComplete();

        verify(repo).findById(ID);
        verify(passwordEncoder).encode("12345678");
        verify(repo).update(eq(ID), any(Customer.class));
        verifyNoMoreInteractions(repo);
    }

    // ---------------- delete ----------------

    @Test
    void delete_shouldFindAndDeleteById() {
        Customer actual = actualCustomerJose();
        when(repo.findById(ID)).thenReturn(Mono.just(actual));
        when(repo.deleteById(ID)).thenReturn(Mono.empty());

        StepVerifier.create(service.delete(ID))
                .verifyComplete();

        verify(repo).findById(ID);
        verify(repo).deleteById(ID);
        verifyNoMoreInteractions(repo);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void delete_whenNotFound_shouldError() {
        when(repo.findById(ID)).thenReturn(Mono.empty());

        StepVerifier.create(service.delete(ID))
                .expectError(NotFoundException.class)
                .verify();

        verify(repo).findById(ID);
        verify(repo, never()).deleteById(any());
        verifyNoMoreInteractions(repo);
        verifyNoInteractions(passwordEncoder);
    }
}
