package com.business.banking.services.application.output.port;

import com.business.banking.services.domain.model.Account;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.UUID;

public interface AccountRepositoryPort {

    Flux<Account> findAll();

    Mono<Account> findByNumber(String number);

    Flux<Account> findByCustomerId(UUID customerId);

    Mono<Boolean> existsByNumber(String number);

    Mono<Account> save(Account a);

    Mono<Void> deleteByNumber(String number);

    Mono<Integer> updateBalance(String number, BigDecimal newBalance);

    Flux<Account> findPage(int offset, int limit);

    Flux<Account> findPageByCustomerId(UUID customerId, int offset, int limit);

    Mono<Long> countAll();

    Mono<Long> countByCustomerId(UUID customerId);
}
