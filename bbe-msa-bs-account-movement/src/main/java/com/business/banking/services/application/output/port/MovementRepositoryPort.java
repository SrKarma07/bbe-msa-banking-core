package com.business.banking.services.application.output.port;

import com.business.banking.services.domain.model.Movement;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.UUID;

public interface MovementRepositoryPort {

    Flux<Movement> findAll();

    Mono<Movement> findById(UUID id);

    Mono<Movement> save(Movement m);

    Mono<Void> deleteById(UUID id);

    Flux<Movement> findByAccount(String accountNumber);

    Flux<Movement> findByAccountAndDateRange(String accountNumber, LocalDate from, LocalDate to);

    Mono<Movement> findFirstByIdempotencyKey(String idempotencyKey);

    Flux<Movement> findPage(int offset, int limit);

    Flux<Movement> findPageByAccount(String accountNumber, int offset, int limit);

    Flux<Movement> findPageByAccountAndDateRange(String accountNumber, LocalDate from, LocalDate to, int offset, int limit);

    Mono<Long> countAll();

    Mono<Long> countByAccount(String accountNumber);

    Mono<Long> countByAccountAndDateRange(String accountNumber, LocalDate from, LocalDate to);

    Flux<Movement> findByAccountNumbersAndDateRange(
            java.util.Collection<String> accountNumbers,
            java.time.LocalDate from,
            java.time.LocalDate to
    );
}
