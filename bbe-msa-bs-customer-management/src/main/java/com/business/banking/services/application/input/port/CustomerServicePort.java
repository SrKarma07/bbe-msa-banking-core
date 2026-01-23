package com.business.banking.services.application.input.port;

import com.business.banking.services.application.shared.PageResult;
import com.business.banking.services.domain.model.Customer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface CustomerServicePort {

    Flux<Customer> list();

    Mono<PageResult<Customer>> listPage(Boolean state, String identification, String name, int page, int size);

    Mono<Customer> getById(UUID id);

    Mono<Customer> create(Customer customer);

    Mono<Customer> update(UUID id, Customer patch);

    Mono<Void> delete(UUID id);
}
