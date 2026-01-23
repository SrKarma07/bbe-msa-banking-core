package com.business.banking.services.application.output.port;

import com.business.banking.services.domain.model.Customer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface CustomerRepositoryPort {

    Flux<Customer> findAll();

    Flux<Customer> findPage(Boolean state, String identification, String name, int offset, int limit);

    Mono<Long> count(Boolean state, String identification, String name);

    Mono<Customer> findById(UUID id);

    Mono<Boolean> existsByIdentification(String identification);

    Mono<Boolean> existsByIdentificationAndDifferentCustomer(UUID customerId, String identification);

    Mono<Customer> create(Customer c);

    Mono<Customer> update(UUID id, Customer merged);

    Mono<Void> deleteById(UUID id);
}
