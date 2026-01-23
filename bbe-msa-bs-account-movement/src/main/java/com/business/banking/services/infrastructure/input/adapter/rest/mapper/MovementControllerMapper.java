package com.business.banking.services.infrastructure.input.adapter.rest.mapper;

import com.business.banking.services.domain.model.Movement;
import com.business.banking.services.infrastructure.input.adapter.rest.dto.MovementCreateRequest;
import com.business.banking.services.infrastructure.input.adapter.rest.dto.MovementResponse;
import com.business.banking.services.infrastructure.input.adapter.rest.dto.MovementUpdateRequest;
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

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "accountNumber", ignore = true)
    @Mapping(target = "balance", ignore = true)
    @Mapping(target = "idempotencyKey", ignore = true)
    Movement toPatch(MovementUpdateRequest req);

    @Mapping(target = "accountNumber", source = "accountNumber")
    @Mapping(target = "balance", source = "balance")
    MovementResponse toResponse(Movement m);
}
