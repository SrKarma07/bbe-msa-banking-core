package com.business.banking.services.application.service;

import com.business.banking.services.application.input.port.AccountServicePort;
import com.business.banking.services.application.output.port.AccountRepositoryPort;
import com.business.banking.services.application.shared.PageResult;
import com.business.banking.services.domain.exception.DuplicateAccountNumberException;
import com.business.banking.services.domain.exception.NotFoundException;
import com.business.banking.services.domain.model.Account;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.UUID;

import static org.apache.commons.lang3.ObjectUtils.firstNonNull;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService implements AccountServicePort {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 50;
    private static final int MAX_SIZE = 200;

    private final AccountRepositoryPort repo;

    @Override
    public Flux<Account> list(UUID customerId) {
        return customerId != null
                ? repo.findByCustomerId(customerId)
                : repo.findAll();
    }

    @Override
    public Mono<PageResult<Account>> listPage(UUID customerId, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = clamp(size, 1, MAX_SIZE, DEFAULT_SIZE);
        int offset = safePage * safeSize;

        Mono<Long> totalMono = customerId != null
                ? repo.countByCustomerId(customerId)
                : repo.countAll();

        Flux<Account> itemsFlux = customerId != null
                ? repo.findPageByCustomerId(customerId, offset, safeSize)
                : repo.findPage(offset, safeSize);

        return totalMono.map(total -> new PageResult<>(itemsFlux, total));
    }

    @Override
    public Mono<Account> getByNumber(String accountNumber) {
        return repo.findByNumber(accountNumber)
                .switchIfEmpty(Mono.error(NotFoundException.accountByNumber(accountNumber)));
    }

    @Override
    public Mono<Account> create(Account a) {
        Objects.requireNonNull(a, "account cannot be null");
        return repo.existsByNumber(a.getNumber())
                .flatMap(exists -> exists
                        ? Mono.error(new DuplicateAccountNumberException(a.getNumber()))
                        : repo.save(a))
                .doOnSuccess(saved -> log.info("[account] created number={}", saved.getNumber()));
    }

    @Override
    public Mono<Account> update(String accountNumber, Account patch) {
        return getByNumber(accountNumber).flatMap(actual -> {
            var p = patch != null ? patch : Account.builder().build();

            var merged = Account.builder()
                    .number(actual.getNumber())
                    .type(firstNonNull(p.getType(), actual.getType()))
                    .balance(firstNonNull(p.getBalance(), actual.getBalance()))
                    .state(firstNonNull(p.getState(), actual.getState()))
                    .customerId(firstNonNull(p.getCustomerId(), actual.getCustomerId()))
                    .build();

            return repo.save(merged);
        }).doOnSuccess(u -> log.info("[account] update number={}", u.getNumber()));
    }

    @Override
    public Mono<Void> delete(String accountNumber) {
        return getByNumber(accountNumber)
                .flatMap(acc -> repo.deleteByNumber(acc.getNumber()))
                .then()
                .doOnSuccess(v -> log.info("[account] deleted number={}", accountNumber));
    }

    private static int clamp(int value, int min, int max, int defaultValue) {
        if (value <= 0) return defaultValue;
        return Math.min(Math.max(value, min), max);
    }
}
