package com.business.banking.services.application.service;

import com.business.banking.services.application.input.port.MovementServicePort;
import com.business.banking.services.application.output.port.AccountRepositoryPort;
import com.business.banking.services.application.output.port.MovementRepositoryPort;
import com.business.banking.services.application.shared.PageResult;
import com.business.banking.services.domain.exception.AccountClosedException;
import com.business.banking.services.domain.exception.InsufficientBalanceException;
import com.business.banking.services.domain.exception.InvalidRequestException;
import com.business.banking.services.domain.exception.NotFoundException;
import com.business.banking.services.domain.model.Account;
import com.business.banking.services.domain.model.Movement;
import com.business.banking.services.domain.model.MovementType;
import com.business.banking.services.domain.policy.MovementPolicy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MovementService implements MovementServicePort {

    private final AccountRepositoryPort accountRepo;
    private final MovementRepositoryPort movementRepo;
    private final TransactionalOperator tx;

    @Override
    public Flux<Movement> list() {
        return movementRepo.findAll();
    }

    @Override
    public Mono<Movement> getById(UUID movementId) {
        Objects.requireNonNull(movementId, "movementId cannot be null");
        return movementRepo.findById(movementId)
                .switchIfEmpty(Mono.error(NotFoundException.movementById(movementId.toString())));
    }

    @Override
    public Mono<Movement> create(Movement m, String idempotencyKey) {
        final Movement normalized;
        try {
            normalized = MovementPolicy.normalizeForPersistence(m);
        } catch (InvalidRequestException ex) {
            return Mono.error(ex);
        }

        String key = StringUtils.trimToNull(idempotencyKey);
        if (key == null) {
            return accountRepo.findByNumber(normalized.getAccountNumber())
                    .switchIfEmpty(Mono.error(NotFoundException.accountByNumber(normalized.getAccountNumber())))
                    .flatMap(acc -> applyMovementAndSaveInSingleTx(acc, normalized, null));
        }

        return movementRepo.findFirstByIdempotencyKey(key)
                .flatMap(existing -> ensureSameIdempotentRequest(existing, normalized, key))
                .switchIfEmpty(Mono.defer(() ->
                        accountRepo.findByNumber(normalized.getAccountNumber())
                                .switchIfEmpty(Mono.error(NotFoundException.accountByNumber(normalized.getAccountNumber())))
                                .flatMap(acc -> applyMovementAndSaveInSingleTx(acc, normalized, key))
                                .onErrorResume(DuplicateKeyException.class, ex ->
                                        movementRepo.findFirstByIdempotencyKey(key)
                                                .switchIfEmpty(Mono.error(ex))
                                                .flatMap(existing -> ensureSameIdempotentRequest(existing, normalized, key))
                                )
                ));
    }

    @Override
    public Mono<Movement> update(UUID movementId, Movement patch) {
        Objects.requireNonNull(movementId, "movementId cannot be null");
        return getById(movementId)
                .flatMap(actual -> {
                    Movement safePatch = patch != null ? patch : Movement.builder().build();
                    Movement merged = actual.toBuilder()
                            .detail(safePatch.getDetail() != null ? safePatch.getDetail() : actual.getDetail())
                            .build();
                    return movementRepo.save(merged);
                })
                .doOnSuccess(u -> log.info("[movement] update id={}", u.getId()));
    }

    @Override
    public Mono<Void> delete(UUID movementId) {
        Objects.requireNonNull(movementId, "movementId cannot be null");
        return getById(movementId)
                .flatMap(found -> movementRepo.deleteById(found.getId()))
                .doOnSuccess(v -> log.info("[movement] deleted id={}", movementId));
    }

    @Override
    public Flux<Movement> listByAccountBetween(String accountNumber, LocalDate from, LocalDate to) {
        if (StringUtils.isBlank(accountNumber)) {
            return Flux.error(new InvalidRequestException("accountNumber is required", null));
        }
        if (from == null || to == null) {
            return Flux.error(new InvalidRequestException("dateFrom and dateTo are required", null));
        }
        return movementRepo.findByAccountAndDateRange(accountNumber, from, to);
    }

    @Override
    public Mono<PageResult<Movement>> listPage(
            String accountNumber,
            LocalDate dateFrom,
            LocalDate dateTo,
            int page,
            int size
    ) {
        int safePage = Math.max(page, 0);
        int safeSize = clamp(size, 1, 200, 50);
        int offset = safePage * safeSize;

        boolean hasAccount = accountNumber != null && !accountNumber.isBlank();
        boolean hasFrom = dateFrom != null;
        boolean hasTo = dateTo != null;

        Mono<Long> totalMono;
        Flux<Movement> itemsFlux;

        if (!hasAccount) {
            totalMono = movementRepo.countAll();
            itemsFlux = movementRepo.findPage(offset, safeSize);
        } else if (hasFrom && hasTo) {
            totalMono = movementRepo.countByAccountAndDateRange(accountNumber, dateFrom, dateTo);
            itemsFlux = movementRepo.findPageByAccountAndDateRange(accountNumber, dateFrom, dateTo, offset, safeSize);
        } else if (!hasFrom && !hasTo) {
            totalMono = movementRepo.countByAccount(accountNumber);
            itemsFlux = movementRepo.findPageByAccount(accountNumber, offset, safeSize);
        } else {
            return Mono.error(new InvalidRequestException("dateFrom and dateTo must be provided together", null));
        }

        return totalMono.map(total -> new PageResult<>(itemsFlux, total));
    }

    private static int clamp(int value, int min, int max, int defaultValue) {
        if (value <= 0) return defaultValue;
        return Math.min(Math.max(value, min), max);
    }

    private Mono<Movement> ensureSameIdempotentRequest(Movement existing, Movement requested, String key) {
        if (isSameIdempotentPayload(existing, requested)) {
            return Mono.just(existing);
        }
        return Mono.error(new InvalidRequestException(
                "Idempotency-Key was already used with a different request payload",
                Map.of(
                        "idempotencyKey", key,
                        "existingMovementId", existing.getId() != null ? existing.getId().toString() : null
                )
        ));
    }

    private boolean isSameIdempotentPayload(Movement existing, Movement requested) {
        if (!safeEquals(existing.getAccountNumber(), requested.getAccountNumber())) return false;
        if (!safeEquals(existing.getDate(), requested.getDate())) return false;
        if (!safeEquals(existing.getType(), requested.getType())) return false;
        if (!safeEquals(existing.getValue(), requested.getValue())) return false;

        String exDetail = StringUtils.trimToNull(existing.getDetail());
        String rqDetail = StringUtils.trimToNull(requested.getDetail());
        return safeEquals(exDetail, rqDetail);
    }

    private static boolean safeEquals(Object a, Object b) {
        return a == null ? b == null : a.equals(b);
    }

    private Mono<Movement> applyMovementAndSaveInSingleTx(Account acc, Movement m, String idempotencyKey) {
        return Mono.defer(() -> {
                    if (Boolean.FALSE.equals(acc.getState())) {
                        return Mono.error(new AccountClosedException(acc.getNumber()));
                    }

                    // IMPORTANT:
                    // - We persist movement.value as POSITIVE always (DB constraint value > 0)
                    // - The effect on the balance is derived from movement.type (Deposito/Retiro)
                    BigDecimal amount = m.getValue() != null ? m.getValue().abs() : BigDecimal.ZERO;

                    final MovementType type;
                    try {
                        type = MovementType.fromApiValue(m.getType());
                    } catch (IllegalArgumentException ex) {
                        return Mono.error(new InvalidRequestException(ex.getMessage(), Map.of("field", "type", "value", m.getType())));
                    }

                    BigDecimal delta = (type == MovementType.WITHDRAWAL) ? amount.negate() : amount;
                    BigDecimal newBalance = acc.getBalance().add(delta);

                    if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
                        // Keep delta (negative for withdrawal) so the exception message can reflect the attempted change
                        return Mono.error(new InsufficientBalanceException(acc.getNumber(), acc.getBalance(), delta));
                    }

                    Movement toSave = m.toBuilder()
                            .value(amount) // persist positive
                            .balance(newBalance)
                            .idempotencyKey(idempotencyKey)
                            .build();

                    return movementRepo.save(toSave)
                            .flatMap(saved -> accountRepo.updateBalance(acc.getNumber(), newBalance).thenReturn(saved))
                            .doOnSuccess(saved -> log.info("[movement] registered account={} type={} amount={} newBalance={}",
                                    acc.getNumber(), type.apiValue(), amount, newBalance));
                })
                .as(tx::transactional);
    }
}
