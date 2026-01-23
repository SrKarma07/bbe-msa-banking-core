package com.business.banking.services.infrastructure.output.repository.impl;

import com.business.banking.services.application.output.port.AccountRepositoryPort;
import com.business.banking.services.domain.model.Account;
import com.business.banking.services.infrastructure.output.repository.entity.AccountEntity;
import com.business.banking.services.infrastructure.output.repository.reactive.AccountR2dbcRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class AccountRepositoryImpl implements AccountRepositoryPort {

    private final AccountR2dbcRepository repo;

    private static AccountEntity toEntity(Account a, boolean isNew) {
        if (a == null) return null;
        return AccountEntity.builder()
                .number(a.getNumber())
                .type(a.getType())
                .balance(a.getBalance())
                .state(a.getState())
                .customerId(a.getCustomerId())
                .isNew(isNew)
                .build();
    }

    private static Account toDomain(AccountEntity e) {
        if (e == null) return null;
        return Account.builder()
                .number(e.getNumber())
                .type(e.getType())
                .balance(e.getBalance())
                .state(e.getState())
                .customerId(e.getCustomerId())
                .build();
    }

    @Override
    public Flux<Account> findAll() {
        return repo.findAll().map(AccountRepositoryImpl::toDomain);
    }

    @Override
    public Mono<Account> findByNumber(String number) {
        return repo.findById(number).map(AccountRepositoryImpl::toDomain);
    }

    @Override
    public Flux<Account> findByCustomerId(UUID customerId) {
        return repo.findAllByCustomerId(customerId).map(AccountRepositoryImpl::toDomain);
    }

    @Override
    public Mono<Boolean> existsByNumber(String number) {
        return repo.existsByNumber(number);
    }

    @Override
    public Mono<Account> save(Account a) {
        return repo.existsById(a.getNumber())
                .flatMap(exists -> repo.save(toEntity(a, !exists)))
                .map(AccountRepositoryImpl::toDomain);
    }

    @Override
    public Mono<Void> deleteByNumber(String number) {
        return repo.deleteById(number);
    }

    @Override
    public Mono<Integer> updateBalance(String number, BigDecimal newBalance) {
        return repo.updateBalance(number, newBalance);
    }

    @Override
    public Flux<Account> findPage(int offset, int limit) {
        return repo.findPage(offset, limit).map(AccountRepositoryImpl::toDomain);
    }

    @Override
    public Flux<Account> findPageByCustomerId(UUID customerId, int offset, int limit) {
        return repo.findPageByCustomerId(customerId, offset, limit).map(AccountRepositoryImpl::toDomain);
    }

    @Override
    public Mono<Long> countAll() {
        return repo.countAll();
    }

    @Override
    public Mono<Long> countByCustomerId(UUID customerId) {
        return repo.countByCustomerId(customerId);
    }
}
