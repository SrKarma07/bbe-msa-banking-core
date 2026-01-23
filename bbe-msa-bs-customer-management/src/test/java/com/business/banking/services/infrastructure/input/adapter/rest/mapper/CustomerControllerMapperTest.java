package com.business.banking.services.infrastructure.input.adapter.rest.mapper;

import com.business.banking.services.domain.model.Customer;
import com.business.banking.services.infrastructure.input.adapter.rest.dto.CustomerCreateRequest;
import com.business.banking.services.infrastructure.input.adapter.rest.dto.CustomerResponse;
import com.business.banking.services.infrastructure.input.adapter.rest.dto.CustomerUpdateRequest;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.junit.jupiter.api.Assertions.*;

class CustomerControllerMapperTest {

    private final CustomerControllerMapper mapper = Mappers.getMapper(CustomerControllerMapper.class);

    @Test
    void toDomain_shouldMapCreateRequestToCustomer_andIgnoreId() {
        CustomerCreateRequest req = new CustomerCreateRequest()
                .name("Ana")
                .gender("F")
                .age(22)
                .identification("CI-123")
                .address("Quito")
                .phoneNumber("0999999999")
                .password("password123")
                .state(true);

        Customer c = mapper.toDomain(req);

        assertNotNull(c);
        assertNull(c.getId());
        assertEquals("password123", c.getPassword());
        assertTrue(c.getState());

        assertNotNull(c.getPerson());
        assertEquals("Ana", c.getPerson().getName());
        assertEquals("F", c.getPerson().getGender());
        assertEquals(22, c.getPerson().getAge());
        assertEquals("CI-123", c.getPerson().getIdentification());
        assertEquals("Quito", c.getPerson().getAddress());
        assertEquals("0999999999", c.getPerson().getPhoneNumber());
    }

    @Test
    void toPartial_shouldMapUpdateRequestToCustomer_andIgnoreId() {
        CustomerUpdateRequest req = new CustomerUpdateRequest()
                .name("New Name")
                .gender("X")
                .age(30)
                .identification("NEW-ID")
                .address("New Address")
                .phoneNumber("0988888888")
                .password("newpassword")
                .state(false);

        Customer c = mapper.toPartial(req);

        assertNotNull(c);
        assertNull(c.getId());
        assertEquals("newpassword", c.getPassword());
        assertFalse(c.getState());

        assertNotNull(c.getPerson());
        assertEquals("New Name", c.getPerson().getName());
        assertEquals("X", c.getPerson().getGender());
        assertEquals(30, c.getPerson().getAge());
        assertEquals("NEW-ID", c.getPerson().getIdentification());
        assertEquals("New Address", c.getPerson().getAddress());
        assertEquals("0988888888", c.getPerson().getPhoneNumber());
    }

    @Test
    void toPartial_whenAllFieldsNull_shouldReturnCustomerWithNulls() {
        CustomerUpdateRequest req = new CustomerUpdateRequest();

        Customer c = mapper.toPartial(req);

        assertNotNull(c);
        assertNull(c.getId());
        assertNull(c.getPassword());
        assertNull(c.getState());

        assertNotNull(c.getPerson());
        assertNull(c.getPerson().getName());
        assertNull(c.getPerson().getGender());
        assertNull(c.getPerson().getAge());
        assertNull(c.getPerson().getIdentification());
        assertNull(c.getPerson().getAddress());
        assertNull(c.getPerson().getPhoneNumber());
    }

    @Test
    void toResponse_shouldFlattenPersonFields() {
        Customer domain = Customer.builder()
                .id(java.util.UUID.fromString("11111111-1111-1111-1111-111111111111"))
                .password("encoded")
                .state(true)
                .person(com.business.banking.services.domain.model.Person.builder()
                        .name("Ana")
                        .gender("F")
                        .age(22)
                        .identification("CI-123")
                        .address("Quito")
                        .phoneNumber("0999999999")
                        .build())
                .build();

        CustomerResponse res = mapper.toResponse(domain);

        assertNotNull(res);
        assertEquals(java.util.UUID.fromString("11111111-1111-1111-1111-111111111111"), res.getId());
        assertEquals("Ana", res.getName());
        assertEquals("F", res.getGender());
        assertEquals(22, res.getAge());
        assertEquals("CI-123", res.getIdentification());
        assertEquals("Quito", res.getAddress());
        assertEquals("0999999999", res.getPhoneNumber());
        assertTrue(res.getState());
    }

    @Test
    void toResponse_whenDomainNull_shouldReturnNull() {
        assertNull(mapper.toResponse(null));
    }

    @Test
    void toDomain_whenDtoNull_shouldReturnNull() {
        assertNull(mapper.toDomain(null));
    }

    @Test
    void toPartial_whenDtoNull_shouldReturnNull() {
        assertNull(mapper.toPartial(null));
    }
}
