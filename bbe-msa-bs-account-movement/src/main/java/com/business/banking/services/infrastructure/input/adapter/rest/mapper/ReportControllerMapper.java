package com.business.banking.services.infrastructure.input.adapter.rest.mapper;

import com.business.banking.services.domain.model.AccountStatement;
import com.business.banking.services.domain.model.AccountSummary;
import com.business.banking.services.domain.model.MovementDetail;
import com.business.banking.services.infrastructure.input.adapter.rest.dto.AccountResponse;
import com.business.banking.services.infrastructure.input.adapter.rest.dto.AccountStatementResponse;
import com.business.banking.services.infrastructure.input.adapter.rest.dto.AccountStatementResponseAccountsInner;
import com.business.banking.services.infrastructure.input.adapter.rest.dto.MovementResponse;
import org.mapstruct.*;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ReportControllerMapper {

    @Mapping(target = "customerId", expression = "java(java.util.UUID.fromString(report.getCustomerId()))")
    @Mapping(target = "dateFrom", source = "dateFrom")
    @Mapping(target = "dateTo", source = "dateTo")
    @Mapping(target = "accounts", source = "accounts")
    AccountStatementResponse toResponse(AccountStatement report);

    @Mapping(target = "account", ignore = true)
    @Mapping(target = "movements", ignore = true)
    AccountStatementResponseAccountsInner toAccountsInner(AccountSummary item);

    @Mapping(target = "accountNumber", source = "item.accountNumber")
    @Mapping(target = "type", source = "item.type")
    @Mapping(target = "state", source = "item.state")
    @Mapping(target = "balance", source = "item.balance")
    @Mapping(target = "customerId", source = "customerId")
    AccountResponse toAccountResponse(AccountSummary item, UUID customerId);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "accountNumber", source = "accountNumber")
    @Mapping(target = "date", source = "detail.date")
    @Mapping(target = "type", source = "detail.type")
    @Mapping(target = "value", source = "detail.value")
    @Mapping(target = "balance", source = "detail.balance")
    MovementResponse toMovementResponse(UUID id, String accountNumber, MovementDetail detail);

    @AfterMapping
    default void fillInner(
            @MappingTarget AccountStatementResponseAccountsInner target,
            AccountSummary source,
            @Context UUID customerId
    ) {
        target.setAccount(toAccountResponse(source, customerId));

        List<MovementResponse> movs = source.getMovements() == null
                ? List.of()
                : source.getMovements().stream()
                .map(d -> toMovementResponse(UUID.randomUUID(), source.getAccountNumber(), d))
                .collect(Collectors.toList());

        target.setMovements(movs);
    }

    default List<AccountStatementResponseAccountsInner> mapAccounts(
            List<AccountSummary> accounts,
            @Context UUID customerId
    ) {
        if (accounts == null) return List.of();
        return accounts.stream()
                .map(a -> {
                    AccountStatementResponseAccountsInner inner = toAccountsInner(a);
                    fillInner(inner, a, customerId);
                    return inner;
                })
                .collect(Collectors.toList());
    }

    @AfterMapping
    default void fixAccounts(
            @MappingTarget AccountStatementResponse target,
            AccountStatement source
    ) {
        UUID customerId = UUID.fromString(source.getCustomerId());
        target.setAccounts(mapAccounts(source.getAccounts(), customerId));
    }
}
