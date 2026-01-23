package com.business.banking.services.application.service;

import com.business.banking.services.application.input.port.ReportServicePort;
import com.business.banking.services.application.output.port.AccountRepositoryPort;
import com.business.banking.services.application.output.port.MovementRepositoryPort;
import com.business.banking.services.domain.exception.InvalidRequestException;
import com.business.banking.services.domain.model.AccountStatement;
import com.business.banking.services.domain.model.AccountSummary;
import com.business.banking.services.domain.model.Movement;
import com.business.banking.services.domain.model.MovementDetail;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService implements ReportServicePort {

    private final AccountRepositoryPort accountRepo;
    private final MovementRepositoryPort movementRepo;

    @Override
    public Mono<AccountStatement> getAccountStatement(String customerId, LocalDate from, LocalDate to) {
        if (StringUtils.isBlank(customerId)) {
            return Mono.error(new InvalidRequestException("customerId is required", null));
        }
        if (from == null || to == null) {
            return Mono.error(new InvalidRequestException("dateFrom and dateTo are required", null));
        }
        if (from.isAfter(to)) {
            return Mono.error(new InvalidRequestException("dateFrom must be <= dateTo", Map.of("dateFrom", from, "dateTo", to)));
        }

        UUID cid;
        try {
            cid = UUID.fromString(customerId);
        } catch (IllegalArgumentException ex) {
            return Mono.error(new InvalidRequestException("customerId must be a valid UUID", Map.of("customerId", customerId)));
        }

        return accountRepo.findByCustomerId(cid)
                .collectList()
                .flatMap(accounts -> {
                    if (accounts.isEmpty()) {
                        return Mono.just(AccountStatement.builder()
                                .customerId(customerId)
                                .dateFrom(from)
                                .dateTo(to)
                                .accounts(List.of())
                                .build());
                    }

                    List<String> numbers = accounts.stream()
                            .map(a -> a.getNumber())
                            .filter(Objects::nonNull)
                            .toList();

                    return movementRepo.findByAccountNumbersAndDateRange(numbers, from, to)
                            .collectList()
                            .map(movements -> buildStatement(customerId, from, to, accounts, movements));
                })
                .doOnSuccess(r -> log.info("[report] statement customer={} accounts={} range=[{},{}]",
                        customerId,
                        r.getAccounts() != null ? r.getAccounts().size() : 0,
                        from, to));
    }

    private AccountStatement buildStatement(
            String customerId,
            LocalDate from,
            LocalDate to,
            List<com.business.banking.services.domain.model.Account> accounts,
            List<Movement> movements
    ) {
        Map<String, List<Movement>> byAccount = movements.stream()
                .collect(Collectors.groupingBy(Movement::getAccountNumber, LinkedHashMap::new, Collectors.toList()));

        List<AccountSummary> summaries = accounts.stream()
                .map(acc -> {
                    List<MovementDetail> details = byAccount.getOrDefault(acc.getNumber(), List.of()).stream()
                            .map(m -> MovementDetail.builder()
                                    .date(m.getDate())
                                    .type(m.getType())
                                    .value(m.getValue())
                                    .balance(m.getBalance())
                                    .build())
                            .toList();

                    return AccountSummary.builder()
                            .accountNumber(acc.getNumber())
                            .type(acc.getType())
                            .state(acc.getState())
                            .balance(acc.getBalance())
                            .movements(details)
                            .build();
                })
                .toList();

        return AccountStatement.builder()
                .customerId(customerId)
                .dateFrom(from)
                .dateTo(to)
                .accounts(summaries)
                .build();
    }
}
