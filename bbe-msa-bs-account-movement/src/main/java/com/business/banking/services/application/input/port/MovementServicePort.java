package com.business.banking.services.application.input.port;

import com.business.banking.services.application.shared.PageResult;
import com.business.banking.services.domain.model.Movement;
import com.business.banking.services.domain.model.Transfer;
import com.business.banking.services.domain.model.TransferResult;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.UUID;

public interface MovementServicePort {

    Flux<Movement> list();

    Mono<Movement> create(Movement movement, String idempotencyKey);

    Mono<TransferResult> transfer(Transfer transfer, String idempotencyKey);

    Flux<Movement> listByAccountBetween(String accountNumber, LocalDate dateFrom, LocalDate dateTo);

    Mono<PageResult<Movement>> listPage(String accountNumber, LocalDate dateFrom, LocalDate dateTo, int page, int size);

    Mono<Movement> getById(UUID id);

    Mono<Movement> update(UUID id, Movement patch);

    Mono<Void> delete(UUID id);
}
