package com.business.banking.services.infrastructure.input.adapter.rest.mapper;

import com.business.banking.services.domain.model.Customer;
import com.business.banking.services.infrastructure.input.adapter.rest.dto.CustomerCreateRequest;
import com.business.banking.services.infrastructure.input.adapter.rest.dto.CustomerResponse;
import com.business.banking.services.infrastructure.input.adapter.rest.dto.CustomerUpdateRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface CustomerControllerMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "person.name", source = "name")
    @Mapping(target = "person.gender", source = "gender")
    @Mapping(target = "person.age", source = "age")
    @Mapping(target = "person.identification", source = "identification")
    @Mapping(target = "person.address", source = "address")
    @Mapping(target = "person.phoneNumber", source = "phoneNumber")
    @Mapping(target = "password", source = "password")
    @Mapping(target = "state", source = "state")
    Customer toDomain(CustomerCreateRequest dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "person.name", source = "name")
    @Mapping(target = "person.gender", source = "gender")
    @Mapping(target = "person.age", source = "age")
    @Mapping(target = "person.identification", source = "identification")
    @Mapping(target = "person.address", source = "address")
    @Mapping(target = "person.phoneNumber", source = "phoneNumber")
    @Mapping(target = "password", source = "password")
    @Mapping(target = "state", source = "state")
    Customer toPartial(CustomerUpdateRequest request);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "person.name")
    @Mapping(target = "gender", source = "person.gender")
    @Mapping(target = "age", source = "person.age")
    @Mapping(target = "identification", source = "person.identification")
    @Mapping(target = "address", source = "person.address")
    @Mapping(target = "phoneNumber", source = "person.phoneNumber")
    @Mapping(target = "state", source = "state")
    CustomerResponse toResponse(Customer domain);
}
