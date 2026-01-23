package com.business.banking.services.infrastructure.input.adapter.rest.mapper;

import com.business.banking.services.domain.model.Account;
import com.business.banking.services.infrastructure.input.adapter.rest.dto.AccountCreateRequest;
import com.business.banking.services.infrastructure.input.adapter.rest.dto.AccountResponse;
import com.business.banking.services.infrastructure.input.adapter.rest.dto.AccountUpdateRequest;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface AccountControllerMapper {

    @Mapping(target = "number", source = "accountNumber")
    @Mapping(target = "type", source = "type")
    @Mapping(target = "balance", source = "initialBalance")
    @Mapping(target = "state", source = "state")
    @Mapping(target = "customerId", source = "customerId")
    Account toDomain(AccountCreateRequest req);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "type", source = "type")
    @Mapping(target = "state", source = "state")
    @Mapping(target = "number", ignore = true)
    @Mapping(target = "balance", ignore = true)
    @Mapping(target = "customerId", ignore = true)
    Account toPatch(AccountUpdateRequest req);

    @Mapping(target = "accountNumber", source = "number")
    @Mapping(target = "type", source = "type")
    @Mapping(target = "state", source = "state")
    @Mapping(target = "balance", source = "balance")
    @Mapping(target = "customerId", source = "customerId")
    AccountResponse toResponse(Account a);
}
