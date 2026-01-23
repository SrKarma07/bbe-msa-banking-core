package com.business.banking.services.infrastructure.output.repository.impl;

import com.business.banking.services.domain.model.Customer;
import com.business.banking.services.domain.model.Person;
import com.business.banking.services.infrastructure.output.repository.entity.CustomerEntity;
import com.business.banking.services.infrastructure.output.repository.entity.PersonEntity;
import com.business.banking.services.infrastructure.output.repository.reactive.CustomerPersonRow;
import com.business.banking.services.infrastructure.output.repository.reactive.CustomerR2dbcRepository;
import com.business.banking.services.infrastructure.output.repository.reactive.PersonR2dbcRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerRepositoryImplTest {

    @Mock
    private PersonR2dbcRepository personRepo;

    @Mock
    private CustomerR2dbcRepository customerRepo;

    @Mock
    private TransactionalOperator tx;

    @InjectMocks
    private CustomerRepositoryImpl repo;

    @BeforeEach
    void setup() {
        lenient().when(tx.transactional(Mockito.<Mono<?>>any()))
                .thenAnswer(inv -> inv.getArgument(0));

        lenient().when(tx.transactional(Mockito.<Flux<?>>any()))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void findAll_shouldJoinCustomerAndPerson() {
        UUID c1 = UUID.randomUUID();
        UUID c2 = UUID.randomUUID();

        CustomerEntity ce1 = CustomerEntity.builder()
                .id(c1)
                .personId(10L)
                .password("p1")
                .state(true)
                .build();

        CustomerEntity ce2 = CustomerEntity.builder()
                .id(c2)
                .personId(20L)
                .password("p2")
                .state(false)
                .build();

        PersonEntity pe1 = PersonEntity.builder()
                .personId(10L)
                .name("Ana")
                .gender("F")
                .age(20)
                .identification("ID-1")
                .address("Addr-1")
                .phoneNumber("099")
                .build();

        PersonEntity pe2 = PersonEntity.builder()
                .personId(20L)
                .name("Bob")
                .gender("M")
                .age(30)
                .identification("ID-2")
                .address("Addr-2")
                .phoneNumber("098")
                .build();

        when(customerRepo.findAll()).thenReturn(Flux.just(ce1, ce2));
        when(personRepo.findById(10L)).thenReturn(Mono.just(pe1));
        when(personRepo.findById(20L)).thenReturn(Mono.just(pe2));

        StepVerifier.create(repo.findAll())
                .assertNext(c -> {
                    assertEquals(c1, c.getId());
                    assertEquals("p1", c.getPassword());
                    assertTrue(c.getState());
                    assertNotNull(c.getPerson());
                    assertEquals("Ana", c.getPerson().getName());
                    assertEquals("F", c.getPerson().getGender());
                    assertEquals(20, c.getPerson().getAge());
                    assertEquals("ID-1", c.getPerson().getIdentification());
                    assertEquals("Addr-1", c.getPerson().getAddress());
                    assertEquals("099", c.getPerson().getPhoneNumber());
                })
                .assertNext(c -> {
                    assertEquals(c2, c.getId());
                    assertEquals("p2", c.getPassword());
                    assertFalse(c.getState());
                    assertNotNull(c.getPerson());
                    assertEquals("Bob", c.getPerson().getName());
                    assertEquals("M", c.getPerson().getGender());
                    assertEquals(30, c.getPerson().getAge());
                    assertEquals("ID-2", c.getPerson().getIdentification());
                    assertEquals("Addr-2", c.getPerson().getAddress());
                    assertEquals("098", c.getPerson().getPhoneNumber());
                })
                .verifyComplete();

        verify(customerRepo).findAll();
        verify(personRepo).findById(10L);
        verify(personRepo).findById(20L);
        verifyNoMoreInteractions(personRepo, customerRepo);
    }

    @Test
    void findPage_shouldMapRowsToDomain() {
        CustomerPersonRow row = mock(CustomerPersonRow.class);
        UUID id = UUID.randomUUID();

        when(row.customerId()).thenReturn(id);
        when(row.name()).thenReturn("Ana");
        when(row.gender()).thenReturn("F");
        when(row.age()).thenReturn(22);
        when(row.identification()).thenReturn("CI");
        when(row.address()).thenReturn("Quito");
        when(row.phoneNumber()).thenReturn("099");
        when(row.password()).thenReturn("enc");
        when(row.state()).thenReturn(true);

        when(customerRepo.findPageRows(true, "CI", "Ana", 0, 10)).thenReturn(Flux.just(row));

        StepVerifier.create(repo.findPage(true, "CI", "Ana", 0, 10))
                .assertNext(c -> {
                    assertEquals(id, c.getId());
                    assertEquals("enc", c.getPassword());
                    assertTrue(c.getState());
                    assertNotNull(c.getPerson());
                    assertEquals("Ana", c.getPerson().getName());
                    assertEquals("F", c.getPerson().getGender());
                    assertEquals(22, c.getPerson().getAge());
                    assertEquals("CI", c.getPerson().getIdentification());
                    assertEquals("Quito", c.getPerson().getAddress());
                    assertEquals("099", c.getPerson().getPhoneNumber());
                })
                .verifyComplete();

        verify(customerRepo).findPageRows(true, "CI", "Ana", 0, 10);
        verifyNoMoreInteractions(personRepo, customerRepo);
    }

    @Test
    void count_shouldDelegateToCustomerRepo() {
        when(customerRepo.countRows(false, null, null)).thenReturn(Mono.just(123L));

        StepVerifier.create(repo.count(false, null, null))
                .expectNext(123L)
                .verifyComplete();

        verify(customerRepo).countRows(false, null, null);
        verifyNoMoreInteractions(personRepo, customerRepo);
    }

    @Test
    void findById_shouldMapRowToDomain() {
        UUID id = UUID.randomUUID();
        CustomerPersonRow row = mock(CustomerPersonRow.class);

        when(row.customerId()).thenReturn(id);
        when(row.name()).thenReturn("Ana");
        when(row.gender()).thenReturn("F");
        when(row.age()).thenReturn(22);
        when(row.identification()).thenReturn("CI");
        when(row.address()).thenReturn("Quito");
        when(row.phoneNumber()).thenReturn("099");
        when(row.password()).thenReturn("enc");
        when(row.state()).thenReturn(true);

        when(customerRepo.findRowById(id)).thenReturn(Mono.just(row));

        StepVerifier.create(repo.findById(id))
                .assertNext(c -> {
                    assertEquals(id, c.getId());
                    assertEquals("enc", c.getPassword());
                    assertTrue(c.getState());
                    assertNotNull(c.getPerson());
                    assertEquals("Ana", c.getPerson().getName());
                })
                .verifyComplete();

        verify(customerRepo).findRowById(id);
        verifyNoMoreInteractions(personRepo, customerRepo);
    }

    @Test
    void existsByIdentification_shouldDelegateToPersonRepo() {
        when(personRepo.existsByIdentification("CI")).thenReturn(Mono.just(true));

        StepVerifier.create(repo.existsByIdentification("CI"))
                .expectNext(true)
                .verifyComplete();

        verify(personRepo).existsByIdentification("CI");
        verifyNoMoreInteractions(personRepo, customerRepo);
    }

    @Test
    void existsByIdentificationAndDifferentCustomer_shouldDelegateToCustomerRepo() {
        UUID id = UUID.randomUUID();
        when(customerRepo.existsByIdentificationAndDifferentCustomer(id, "CI")).thenReturn(Mono.just(false));

        StepVerifier.create(repo.existsByIdentificationAndDifferentCustomer(id, "CI"))
                .expectNext(false)
                .verifyComplete();

        verify(customerRepo).existsByIdentificationAndDifferentCustomer(id, "CI");
        verifyNoMoreInteractions(personRepo, customerRepo);
    }

    @Test
    void create_shouldSavePersonThenCustomer_andReturnDomain_andUseTransactionalOperator() {
        UUID id = UUID.randomUUID();

        Customer input = Customer.builder()
                .id(id)
                .person(Person.builder()
                        .name("Ana")
                        .gender("F")
                        .age(22)
                        .identification("CI")
                        .address("Quito")
                        .phoneNumber("099")
                        .build())
                .password("enc")
                .state(true)
                .build();

        PersonEntity savedPe = PersonEntity.builder()
                .personId(77L)
                .name("Ana")
                .gender("F")
                .age(22)
                .identification("CI")
                .address("Quito")
                .phoneNumber("099")
                .build();

        CustomerEntity savedCe = CustomerEntity.builder()
                .id(id)
                .personId(77L)
                .password("enc")
                .state(true)
                .build();

        ArgumentCaptor<PersonEntity> personCaptor = ArgumentCaptor.forClass(PersonEntity.class);
        ArgumentCaptor<CustomerEntity> customerCaptor = ArgumentCaptor.forClass(CustomerEntity.class);

        when(personRepo.save(personCaptor.capture())).thenReturn(Mono.just(savedPe));
        when(customerRepo.save(customerCaptor.capture())).thenReturn(Mono.just(savedCe));

        StepVerifier.create(repo.create(input))
                .assertNext(out -> {
                    assertEquals(id, out.getId());
                    assertEquals("enc", out.getPassword());
                    assertTrue(out.getState());
                    assertNotNull(out.getPerson());
                    assertEquals("Ana", out.getPerson().getName());
                    assertEquals("CI", out.getPerson().getIdentification());
                })
                .verifyComplete();

        PersonEntity toSavePe = personCaptor.getValue();
        assertNotNull(toSavePe);
        assertEquals("Ana", toSavePe.getName());
        assertEquals("F", toSavePe.getGender());
        assertEquals(22, toSavePe.getAge());
        assertEquals("CI", toSavePe.getIdentification());
        assertEquals("Quito", toSavePe.getAddress());
        assertEquals("099", toSavePe.getPhoneNumber());
        assertTrue(toSavePe.isNew());

        CustomerEntity toSaveCe = customerCaptor.getValue();
        assertNotNull(toSaveCe);
        assertEquals(id, toSaveCe.getId());
        assertEquals(77L, toSaveCe.getPersonId());
        assertEquals("enc", toSaveCe.getPassword());
        assertTrue(toSaveCe.getState());
        assertTrue(toSaveCe.isNew());

        verify(tx, times(1)).transactional(Mockito.any(Mono.class));
    }

    @Test
    void update_shouldLoadCustomerAndPerson_updateFields_saveBoth_andReturnDomain_andUseTransactionalOperator() {
        UUID id = UUID.randomUUID();

        CustomerEntity existingCe = CustomerEntity.builder()
                .id(id)
                .personId(10L)
                .password("old-enc")
                .state(false)
                .build();

        PersonEntity existingPe = PersonEntity.builder()
                .personId(10L)
                .name("Old")
                .gender("M")
                .age(40)
                .identification("OLD")
                .address("OldAddr")
                .phoneNumber("000")
                .build();

        Customer merged = Customer.builder()
                .id(id)
                .person(Person.builder()
                        .name("New")
                        .gender("F")
                        .age(21)
                        .identification("NEW")
                        .address("NewAddr")
                        .phoneNumber("999")
                        .build())
                .password("new-enc")
                .state(true)
                .build();

        ArgumentCaptor<PersonEntity> peCaptor = ArgumentCaptor.forClass(PersonEntity.class);
        ArgumentCaptor<CustomerEntity> ceCaptor = ArgumentCaptor.forClass(CustomerEntity.class);

        when(customerRepo.findById(id)).thenReturn(Mono.just(existingCe));
        when(personRepo.findById(10L)).thenReturn(Mono.just(existingPe));
        when(personRepo.save(peCaptor.capture())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(customerRepo.save(ceCaptor.capture())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(repo.update(id, merged))
                .assertNext(out -> {
                    assertEquals(id, out.getId());
                    assertEquals("new-enc", out.getPassword());
                    assertTrue(out.getState());
                    assertNotNull(out.getPerson());
                    assertEquals("New", out.getPerson().getName());
                    assertEquals("F", out.getPerson().getGender());
                    assertEquals(21, out.getPerson().getAge());
                    assertEquals("NEW", out.getPerson().getIdentification());
                    assertEquals("NewAddr", out.getPerson().getAddress());
                    assertEquals("999", out.getPerson().getPhoneNumber());
                })
                .verifyComplete();

        PersonEntity savedPe = peCaptor.getValue();
        assertEquals(10L, savedPe.getPersonId());
        assertEquals("New", savedPe.getName());
        assertEquals("F", savedPe.getGender());
        assertEquals(21, savedPe.getAge());
        assertEquals("NEW", savedPe.getIdentification());
        assertEquals("NewAddr", savedPe.getAddress());
        assertEquals("999", savedPe.getPhoneNumber());
        assertFalse(savedPe.isNew());

        CustomerEntity savedCe = ceCaptor.getValue();
        assertEquals(id, savedCe.getId());
        assertEquals(10L, savedCe.getPersonId());
        assertEquals("new-enc", savedCe.getPassword());
        assertTrue(savedCe.getState());
        assertFalse(savedCe.isNew());

        verify(tx, times(1)).transactional(Mockito.any(Mono.class));
    }

    @Test
    void update_whenCustomerNotFound_shouldError() {
        UUID id = UUID.randomUUID();
        when(customerRepo.findById(id)).thenReturn(Mono.empty());

        Customer merged = Customer.builder()
                .id(id)
                .person(Person.builder()
                        .name("New")
                        .gender("F")
                        .age(21)
                        .identification("NEW")
                        .address("NewAddr")
                        .phoneNumber("999")
                        .build())
                .password("new-enc")
                .state(true)
                .build();

        StepVerifier.create(repo.update(id, merged))
                .expectErrorSatisfies(err -> {
                    assertTrue(err instanceof IllegalStateException);
                    assertTrue(err.getMessage().contains("Customer not found for update"));
                })
                .verify();

        verify(customerRepo).findById(id);
        verify(tx, times(1)).transactional(Mockito.any(Mono.class));
        verifyNoMoreInteractions(personRepo, customerRepo);
    }

    @Test
    void update_whenPersonNotFound_shouldError() {
        UUID id = UUID.randomUUID();
        CustomerEntity existingCe = CustomerEntity.builder()
                .id(id)
                .personId(10L)
                .password("old-enc")
                .state(false)
                .build();

        when(customerRepo.findById(id)).thenReturn(Mono.just(existingCe));
        when(personRepo.findById(10L)).thenReturn(Mono.empty());

        Customer merged = Customer.builder()
                .id(id)
                .person(Person.builder()
                        .name("New")
                        .gender("F")
                        .age(21)
                        .identification("NEW")
                        .address("NewAddr")
                        .phoneNumber("999")
                        .build())
                .password("new-enc")
                .state(true)
                .build();

        StepVerifier.create(repo.update(id, merged))
                .expectErrorSatisfies(err -> {
                    assertTrue(err instanceof IllegalStateException);
                    assertTrue(err.getMessage().contains("Person not found for customer"));
                })
                .verify();

        verify(customerRepo).findById(id);
        verify(personRepo).findById(10L);
        verify(tx, times(1)).transactional(Mockito.any(Mono.class));
        verifyNoMoreInteractions(personRepo, customerRepo);
    }

    @Test
    void deleteById_shouldDelegateToCustomerRepo() {
        UUID id = UUID.randomUUID();
        when(customerRepo.deleteById(id)).thenReturn(Mono.empty());

        StepVerifier.create(repo.deleteById(id))
                .verifyComplete();

        verify(customerRepo).deleteById(id);
        verifyNoMoreInteractions(personRepo, customerRepo);
    }
}
