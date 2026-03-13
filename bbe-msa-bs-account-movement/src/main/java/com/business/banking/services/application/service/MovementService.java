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
import com.business.banking.services.domain.model.Transfer;
import com.business.banking.services.domain.model.TransferResult;
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
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MovementService implements MovementServicePort {

    private static final String TRANSFER_DEBIT_SUFFIX = ":debit";
    private static final String TRANSFER_CREDIT_SUFFIX = ":credit";

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
    public Mono<TransferResult> transfer(Transfer transfer, String idempotencyKey) {
        final Transfer normalized;
        try {
            normalized = normalizeTransfer(transfer);
        } catch (InvalidRequestException ex) {
            return Mono.error(ex);
        }

        String key = StringUtils.trimToNull(idempotencyKey);
        if (key == null) {
            return findTransferAccounts(normalized)
                    .flatMap(accounts -> applyTransferAndSaveInSingleTx(accounts.source(), accounts.destination(), normalized, null));
        }

        return findExistingTransferByKey(key)
                .flatMap(existing -> ensureSameIdempotentTransfer(existing, normalized, key))
                .switchIfEmpty(Mono.defer(() ->
                        findTransferAccounts(normalized)
                                .flatMap(accounts -> applyTransferAndSaveInSingleTx(accounts.source(), accounts.destination(), normalized, key))
                                .onErrorResume(DuplicateKeyException.class, ex ->
                                        findExistingTransferByKey(key)
                                                .switchIfEmpty(Mono.error(ex))
                                                .flatMap(existing -> ensureSameIdempotentTransfer(existing, normalized, key))
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

    private Mono<TransferResult> ensureSameIdempotentTransfer(TransferResult existing, Transfer requested, String key) {
        if (isSameIdempotentTransfer(existing, requested)) {
            return Mono.just(existing);
        }

        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("idempotencyKey", key);
        attributes.put("existingDebitMovementId", existing.getDebitMovement() != null && existing.getDebitMovement().getId() != null
                ? existing.getDebitMovement().getId().toString()
                : null);
        attributes.put("existingCreditMovementId", existing.getCreditMovement() != null && existing.getCreditMovement().getId() != null
                ? existing.getCreditMovement().getId().toString()
                : null);

        return Mono.error(new InvalidRequestException(
                "Idempotency-Key was already used with a different transfer payload",
                attributes
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

    private boolean isSameIdempotentTransfer(TransferResult existing, Transfer requested) {
        if (existing == null || existing.getDebitMovement() == null || existing.getCreditMovement() == null) {
            return false;
        }

        Movement debit = existing.getDebitMovement();
        Movement credit = existing.getCreditMovement();
        BigDecimal amount = requested.getValue().abs();

        if (!safeEquals(debit.getAccountNumber(), requested.getSourceAccountNumber())) return false;
        if (!safeEquals(credit.getAccountNumber(), requested.getDestinationAccountNumber())) return false;
        if (!safeEquals(debit.getDate(), requested.getDate())) return false;
        if (!safeEquals(credit.getDate(), requested.getDate())) return false;
        if (!safeEquals(debit.getType(), MovementType.WITHDRAWAL.apiValue())) return false;
        if (!safeEquals(credit.getType(), MovementType.DEPOSIT.apiValue())) return false;
        if (!safeEquals(debit.getValue(), amount)) return false;
        if (!safeEquals(credit.getValue(), amount)) return false;
        if (!safeEquals(StringUtils.trimToNull(debit.getDetail()), buildTransferDebitDetail(requested))) return false;
        return safeEquals(StringUtils.trimToNull(credit.getDetail()), buildTransferCreditDetail(requested));
    }

    private static boolean safeEquals(Object a, Object b) {
        return a == null ? b == null : a.equals(b);
    }

    private Mono<Movement> applyMovementAndSaveInSingleTx(Account acc, Movement m, String idempotencyKey) {
        return Mono.defer(() -> {
                    if (Boolean.FALSE.equals(acc.getState())) {
                        return Mono.error(new AccountClosedException(acc.getNumber()));
                    }

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
                        return Mono.error(new InsufficientBalanceException(acc.getNumber(), acc.getBalance(), delta));
                    }

                    Movement toSave = m.toBuilder()
                            .value(amount)
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

    private Mono<TransferResult> applyTransferAndSaveInSingleTx(
            Account source,
            Account destination,
            Transfer transfer,
            String idempotencyKey
    ) {
        return Mono.defer(() -> {
                    validateTransferAccounts(source, destination);

                    BigDecimal amount = transfer.getValue().abs();
                    BigDecimal sourceNewBalance = source.getBalance().subtract(amount);
                    if (sourceNewBalance.compareTo(BigDecimal.ZERO) < 0) {
                        return Mono.error(new InsufficientBalanceException(source.getNumber(), source.getBalance(), amount.negate()));
                    }
                    BigDecimal destinationNewBalance = destination.getBalance().add(amount);

                    String debitKey = transferDebitKey(idempotencyKey);
                    String creditKey = transferCreditKey(idempotencyKey);

                    Movement debitMovement = Movement.builder()
                            .accountNumber(source.getNumber())
                            .date(transfer.getDate())
                            .type(MovementType.WITHDRAWAL.apiValue())
                            .value(amount)
                            .balance(sourceNewBalance)
                            .detail(buildTransferDebitDetail(transfer))
                            .idempotencyKey(debitKey)
                            .build();

                    Movement creditMovement = Movement.builder()
                            .accountNumber(destination.getNumber())
                            .date(transfer.getDate())
                            .type(MovementType.DEPOSIT.apiValue())
                            .value(amount)
                            .balance(destinationNewBalance)
                            .detail(buildTransferCreditDetail(transfer))
                            .idempotencyKey(creditKey)
                            .build();

                    return movementRepo.save(debitMovement)
                            .flatMap(savedDebit -> movementRepo.save(creditMovement)
                                    .flatMap(savedCredit -> accountRepo.updateBalance(source.getNumber(), sourceNewBalance)
                                            .then(accountRepo.updateBalance(destination.getNumber(), destinationNewBalance))
                                            .thenReturn(TransferResult.builder()
                                                    .debitMovement(savedDebit)
                                                    .creditMovement(savedCredit)
                                                    .build())))
                            .doOnSuccess(result -> log.info(
                                    "[transfer] sourceAccount={} destinationAccount={} amount={} sourceBalance={} destinationBalance={}",
                                    source.getNumber(),
                                    destination.getNumber(),
                                    amount,
                                    sourceNewBalance,
                                    destinationNewBalance
                            ));
                })
                .as(tx::transactional);
    }

    private Transfer normalizeTransfer(Transfer transfer) {
        if (transfer == null) {
            throw new InvalidRequestException("transfer is required", Map.of());
        }
        if (StringUtils.isBlank(transfer.getSourceAccountNumber())) {
            throw new InvalidRequestException("sourceAccountNumber is required", Map.of("field", "sourceAccountNumber"));
        }
        if (StringUtils.isBlank(transfer.getDestinationAccountNumber())) {
            throw new InvalidRequestException("destinationAccountNumber is required", Map.of("field", "destinationAccountNumber"));
        }
        if (transfer.getDate() == null) {
            throw new InvalidRequestException("date is required", Map.of("field", "date"));
        }
        if (transfer.getValue() == null) {
            throw new InvalidRequestException("value is required", Map.of("field", "value"));
        }

        BigDecimal amount = transfer.getValue().abs();
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidRequestException("value must be greater than zero", Map.of("field", "value"));
        }

        String sourceAccountNumber = transfer.getSourceAccountNumber().trim();
        String destinationAccountNumber = transfer.getDestinationAccountNumber().trim();
        if (sourceAccountNumber.equalsIgnoreCase(destinationAccountNumber)) {
            throw new InvalidRequestException(
                    "sourceAccountNumber and destinationAccountNumber must be different",
                    Map.of(
                            "sourceAccountNumber", sourceAccountNumber,
                            "destinationAccountNumber", destinationAccountNumber
                    )
            );
        }

        return transfer.toBuilder()
                .sourceAccountNumber(sourceAccountNumber)
                .destinationAccountNumber(destinationAccountNumber)
                .value(amount)
                .detail(StringUtils.trimToNull(transfer.getDetail()))
                .build();
    }

    private Mono<TransferAccounts> findTransferAccounts(Transfer transfer) {
        Mono<Account> sourceMono = accountRepo.findByNumber(transfer.getSourceAccountNumber())
                .switchIfEmpty(Mono.error(NotFoundException.accountByNumber(transfer.getSourceAccountNumber())));

        Mono<Account> destinationMono = accountRepo.findByNumber(transfer.getDestinationAccountNumber())
                .switchIfEmpty(Mono.error(NotFoundException.accountByNumber(transfer.getDestinationAccountNumber())));

        return Mono.zip(sourceMono, destinationMono)
                .map(tuple -> new TransferAccounts(tuple.getT1(), tuple.getT2()));
    }

    private void validateTransferAccounts(Account source, Account destination) {
        if (Boolean.FALSE.equals(source.getState())) {
            throw new AccountClosedException(source.getNumber());
        }
        if (Boolean.FALSE.equals(destination.getState())) {
            throw new AccountClosedException(destination.getNumber());
        }
    }

    private Mono<TransferResult> findExistingTransferByKey(String idempotencyKey) {
        return Mono.zip(
                        movementRepo.findFirstByIdempotencyKey(transferDebitKey(idempotencyKey))
                                .map(Optional::of)
                                .defaultIfEmpty(Optional.empty()),
                        movementRepo.findFirstByIdempotencyKey(transferCreditKey(idempotencyKey))
                                .map(Optional::of)
                                .defaultIfEmpty(Optional.empty())
                )
                .flatMap(tuple -> {
                    Optional<Movement> debitOptional = tuple.getT1();
                    Optional<Movement> creditOptional = tuple.getT2();

                    if (debitOptional.isPresent() && creditOptional.isPresent()) {
                        return Mono.just(TransferResult.builder()
                                .debitMovement(debitOptional.get())
                                .creditMovement(creditOptional.get())
                                .build());
                    }
                    if (debitOptional.isEmpty() && creditOptional.isEmpty()) {
                        return Mono.empty();
                    }
                    return Mono.error(new InvalidRequestException(
                            "Idempotency-Key is in an inconsistent state for transfer retry",
                            Map.of("idempotencyKey", idempotencyKey)
                    ));
                });
    }

    private String buildTransferDebitDetail(Transfer transfer) {
        String customDetail = StringUtils.trimToNull(transfer.getDetail());
        String base = "Transfer to " + transfer.getDestinationAccountNumber();
        return customDetail == null ? base : base + " - " + customDetail;
    }

    private String buildTransferCreditDetail(Transfer transfer) {
        String customDetail = StringUtils.trimToNull(transfer.getDetail());
        String base = "Transfer from " + transfer.getSourceAccountNumber();
        return customDetail == null ? base : base + " - " + customDetail;
    }

    private String transferDebitKey(String idempotencyKey) {
        return deriveTransferIdempotencyKey(idempotencyKey, TRANSFER_DEBIT_SUFFIX);
    }

    private String transferCreditKey(String idempotencyKey) {
        return deriveTransferIdempotencyKey(idempotencyKey, TRANSFER_CREDIT_SUFFIX);
    }

    private String deriveTransferIdempotencyKey(String idempotencyKey, String suffix) {
        String key = StringUtils.trimToNull(idempotencyKey);
        if (key == null) {
            return null;
        }
        return UUID.nameUUIDFromBytes((key + suffix).getBytes(StandardCharsets.UTF_8)).toString();
    }

    private record TransferAccounts(Account source, Account destination) {
    }
}
