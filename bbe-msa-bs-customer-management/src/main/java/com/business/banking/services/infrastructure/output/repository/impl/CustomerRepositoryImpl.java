package com.business.banking.services.infrastructure.output.repository.impl;

import com.business.banking.services.application.output.port.CustomerRepositoryPort;
import com.business.banking.services.domain.model.Customer;
import com.business.banking.services.domain.model.Person;
import com.business.banking.services.infrastructure.output.repository.entity.CustomerEntity;
import com.business.banking.services.infrastructure.output.repository.entity.PersonEntity;
import com.business.banking.services.infrastructure.output.repository.reactive.CustomerPersonRow;
import com.business.banking.services.infrastructure.output.repository.reactive.CustomerR2dbcRepository;
import com.business.banking.services.infrastructure.output.repository.reactive.PersonR2dbcRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class CustomerRepositoryImpl implements CustomerRepositoryPort {

    private final PersonR2dbcRepository personRepo;
    private final CustomerR2dbcRepository customerRepo;
    private final TransactionalOperator tx;

    private static Customer toDomain(CustomerPersonRow row) {
        if (row == null) return null;
        return Customer.builder()
                .id(row.customerId())
                .person(Person.builder()
                        .name(row.name())
                        .gender(row.gender())
                        .age(row.age())
                        .identification(row.identification())
                        .address(row.address())
                        .phoneNumber(row.phoneNumber())
                        .build())
                .password(row.password())
                .state(row.state())
                .build();
    }

    private static Customer toDomain(PersonEntity pe, CustomerEntity ce) {
        if (pe == null || ce == null) return null;
        return Customer.builder()
                .id(ce.getId())
                .person(Person.builder()
                        .name(pe.getName())
                        .gender(pe.getGender())
                        .age(pe.getAge())
                        .identification(pe.getIdentification())
                        .address(pe.getAddress())
                        .phoneNumber(pe.getPhoneNumber())
                        .build())
                .password(ce.getPassword())
                .state(ce.getState())
                .build();
    }

    private static PersonEntity toNewPersonEntity(Person p) {
        if (p == null) return null;
        return PersonEntity.builder()
                .isNew(true)
                .name(p.getName())
                .gender(p.getGender())
                .age(p.getAge())
                .identification(p.getIdentification())
                .address(p.getAddress())
                .phoneNumber(p.getPhoneNumber())
                .build();
    }

    @Override
    public Flux<Customer> findAll() {
        return customerRepo.findAll()
                .flatMap(ce -> personRepo.findById(ce.getPersonId())
                        .map(pe -> toDomain(pe, ce)));
    }

    @Override
    public Flux<Customer> findPage(Boolean state, String identification, String name, int offset, int limit) {
        return customerRepo.findPageRows(state, identification, name, offset, limit)
                .map(CustomerRepositoryImpl::toDomain);
    }

    @Override
    public Mono<Long> count(Boolean state, String identification, String name) {
        return customerRepo.countRows(state, identification, name);
    }

    @Override
    public Mono<Customer> findById(UUID id) {
        return customerRepo.findRowById(id)
                .map(CustomerRepositoryImpl::toDomain);
    }

    @Override
    public Mono<Boolean> existsByIdentification(String identification) {
        return personRepo.existsByIdentification(identification);
    }

    @Override
    public Mono<Boolean> existsByIdentificationAndDifferentCustomer(UUID customerId, String identification) {
        return customerRepo.existsByIdentificationAndDifferentCustomer(customerId, identification);
    }

    @Override
    public Mono<Customer> create(Customer c) {
        return Mono.defer(() -> {
            PersonEntity pe = toNewPersonEntity(c.getPerson());

            return personRepo.save(pe)
                    .flatMap(savedPe -> {
                        CustomerEntity ce = CustomerEntity.builder()
                                .id(c.getId())
                                .personId(savedPe.getPersonId())
                                .password(c.getPassword())
                                .state(c.getState())
                                .isNew(true)
                                .build();

                        return customerRepo.save(ce)
                                .map(savedCe -> toDomain(savedPe, savedCe));
                    });
        }).as(tx::transactional);
    }

    @Override
    public Mono<Customer> update(UUID id, Customer merged) {
        return Mono.defer(() ->
                customerRepo.findById(id)
                        .switchIfEmpty(Mono.error(new IllegalStateException("Customer not found for update: " + id)))
                        .flatMap(existingCe ->
                                personRepo.findById(existingCe.getPersonId())
                                        .switchIfEmpty(Mono.error(new IllegalStateException("Person not found for customer: " + id)))
                                        .flatMap(existingPe -> {
                                            existingPe.setName(merged.getPerson().getName());
                                            existingPe.setGender(merged.getPerson().getGender());
                                            existingPe.setAge(merged.getPerson().getAge());
                                            existingPe.setIdentification(merged.getPerson().getIdentification());
                                            existingPe.setAddress(merged.getPerson().getAddress());
                                            existingPe.setPhoneNumber(merged.getPerson().getPhoneNumber());
                                            existingPe.setNew(false);

                                            return personRepo.save(existingPe)
                                                    .flatMap(savedPe -> {
                                                        existingCe.setPassword(merged.getPassword());
                                                        existingCe.setState(merged.getState());
                                                        existingCe.setNew(false);

                                                        return customerRepo.save(existingCe)
                                                                .map(savedCe -> toDomain(savedPe, savedCe));
                                                    });
                                        })
                        )
        ).as(tx::transactional);
    }

    @Override
    public Mono<Void> deleteById(UUID id) {
        return customerRepo.deleteById(id);
    }
}
