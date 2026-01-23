package com.business.banking.services.infrastructure.output.repository.impl;

import com.business.banking.services.application.output.port.MovementRepositoryPort;
import com.business.banking.services.domain.model.Movement;
import com.business.banking.services.infrastructure.output.repository.entity.MovementEntity;
import com.business.banking.services.infrastructure.output.repository.reactive.MovementR2dbcRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.Collection;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class MovementRepositoryImpl implements MovementRepositoryPort {

    private final MovementR2dbcRepository repo;

    private static MovementEntity toEntity(Movement m) {
        if (m == null) return null;
        return MovementEntity.builder()
                .id(m.getId())
                .accountNumber(m.getAccountNumber())
                .date(m.getDate())
                .type(m.getType())
                .value(m.getValue())
                .balance(m.getBalance())
                .detail(m.getDetail())
                .idempotencyKey(m.getIdempotencyKey())
                .build();
    }

    private static Movement toDomain(MovementEntity e) {
        if (e == null) return null;
        return Movement.builder()
                .id(e.getId())
                .accountNumber(e.getAccountNumber())
                .date(e.getDate())
                .type(e.getType())
                .value(e.getValue())
                .balance(e.getBalance())
                .detail(e.getDetail())
                .idempotencyKey(e.getIdempotencyKey())
                .build();
    }

    @Override
    public Flux<Movement> findAll() {
        return repo.findAll().map(MovementRepositoryImpl::toDomain);
    }

    @Override
    public Mono<Movement> findById(UUID id) {
        return repo.findById(id).map(MovementRepositoryImpl::toDomain);
    }

    @Override
    public Mono<Movement> save(Movement m) {
        return repo.save(toEntity(m)).map(MovementRepositoryImpl::toDomain);
    }

    @Override
    public Mono<Void> deleteById(UUID id) {
        return repo.deleteById(id);
    }

    @Override
    public Flux<Movement> findByAccount(String accountNumber) {
        return repo.findAllByAccountNumberOrderByDateAsc(accountNumber)
                .map(MovementRepositoryImpl::toDomain);
    }

    @Override
    public Flux<Movement> findByAccountAndDateRange(String accountNumber, LocalDate from, LocalDate to) {
        return repo.findAllByAccountNumberAndDateBetweenOrderByDateAsc(accountNumber, from, to)
                .map(MovementRepositoryImpl::toDomain);
    }

    @Override
    public Mono<Movement> findFirstByIdempotencyKey(String idempotencyKey) {
        return repo.findFirstByIdempotencyKey(idempotencyKey)
                .map(MovementRepositoryImpl::toDomain);
    }

    @Override
    public Flux<Movement> findPage(int offset, int limit) {
        return repo.findPage(offset, limit).map(MovementRepositoryImpl::toDomain);
    }

    @Override
    public Flux<Movement> findPageByAccount(String accountNumber, int offset, int limit) {
        return repo.findPageByAccount(accountNumber, offset, limit)
                .map(MovementRepositoryImpl::toDomain);
    }

    @Override
    public Flux<Movement> findPageByAccountAndDateRange(String accountNumber, LocalDate from, LocalDate to, int offset, int limit) {
        return repo.findPageByAccountAndDateRange(accountNumber, from, to, offset, limit)
                .map(MovementRepositoryImpl::toDomain);
    }

    @Override
    public Mono<Long> countAll() {
        return repo.countAll();
    }

    @Override
    public Mono<Long> countByAccount(String accountNumber) {
        return repo.countByAccount(accountNumber);
    }

    @Override
    public Mono<Long> countByAccountAndDateRange(String accountNumber, LocalDate from, LocalDate to) {
        return repo.countByAccountAndDateRange(accountNumber, from, to);
    }

    @Override
    public Flux<Movement> findByAccountNumbersAndDateRange(Collection<String> accountNumbers, LocalDate from, LocalDate to) {
        if (accountNumbers == null || accountNumbers.isEmpty()) {
            return Flux.empty();
        }
        return repo.findAllByAccountNumbersAndDateRange(accountNumbers, from, to)
                .map(MovementRepositoryImpl::toDomain);
    }
}
