package com.business.banking.services.infrastructure.input.adapter.rest.mapper;

import com.business.banking.services.domain.model.Movement;
import com.business.banking.services.domain.model.Transfer;
import com.business.banking.services.domain.model.TransferResult;
import com.business.banking.services.infrastructure.input.adapter.rest.dto.MovementCreateRequest;
import com.business.banking.services.infrastructure.input.adapter.rest.dto.MovementResponse;
import com.business.banking.services.infrastructure.input.adapter.rest.dto.MovementUpdateRequest;
import com.business.banking.services.infrastructure.input.adapter.rest.dto.TransferCreateRequest;
import com.business.banking.services.infrastructure.input.adapter.rest.dto.TransferResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface MovementControllerMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "accountNumber", source = "accountNumber")
    @Mapping(target = "date", source = "date")
    @Mapping(target = "type", source = "type")
    @Mapping(target = "value", source = "value")
    @Mapping(target = "balance", ignore = true)
    @Mapping(target = "detail", source = "detail")
    @Mapping(target = "idempotencyKey", ignore = true)
    Movement toDomain(MovementCreateRequest req);

    @Mapping(target = "sourceAccountNumber", source = "sourceAccountNumber")
    @Mapping(target = "destinationAccountNumber", source = "destinationAccountNumber")
    @Mapping(target = "date", source = "date")
    @Mapping(target = "value", source = "value")
    @Mapping(target = "detail", source = "detail")
    Transfer toDomain(TransferCreateRequest req);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "accountNumber", ignore = true)
    @Mapping(target = "balance", ignore = true)
    @Mapping(target = "idempotencyKey", ignore = true)
    Movement toPatch(MovementUpdateRequest req);

    @Mapping(target = "accountNumber", source = "accountNumber")
    @Mapping(target = "balance", source = "balance")
    MovementResponse toResponse(Movement m);

    default TransferResponse toResponse(TransferResult result) {
        if (result == null) {
            return null;
        }
        return new TransferResponse()
                .debitMovement(toResponse(result.getDebitMovement()))
                .creditMovement(toResponse(result.getCreditMovement()));
    }
}
