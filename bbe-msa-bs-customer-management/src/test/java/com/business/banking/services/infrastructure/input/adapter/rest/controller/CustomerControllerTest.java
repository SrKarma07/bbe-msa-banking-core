package com.business.banking.services.infrastructure.input.adapter.rest.controller;

import com.business.banking.services.application.input.port.CustomerServicePort;
import com.business.banking.services.application.shared.PageResult;
import com.business.banking.services.domain.model.Customer;
import com.business.banking.services.domain.model.Person;
import com.business.banking.services.infrastructure.input.adapter.rest.dto.CustomerCreateRequest;
import com.business.banking.services.infrastructure.input.adapter.rest.dto.CustomerResponse;
import com.business.banking.services.infrastructure.input.adapter.rest.dto.CustomerUpdateRequest;
import com.business.banking.services.infrastructure.input.adapter.rest.mapper.CustomerControllerMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.URI;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerControllerTest {

    @Mock
    CustomerServicePort service;

    @Mock
    CustomerControllerMapper mapper;

    @InjectMocks
    CustomerController controller;

    private static final UUID ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private static Customer domainCustomer(UUID id, String identification) {
        return Customer.builder()
                .id(id)
                .person(Person.builder()
                        .name("Jose Lema")
                        .gender("M")
                        .age(30)
                        .identification(identification)
                        .address("Otavalo sn y principal")
                        .phoneNumber("098254785")
                        .build())
                .password("ENC")
                .state(true)
                .build();
    }

    private static ServerWebExchange exchange(String uri) {
        return MockServerWebExchange.from(MockServerHttpRequest.get(String.valueOf(URI.create(uri))).build());
    }

    @Test
    void createCustomer_success_shouldReturn201() {
        CustomerCreateRequest req = mock(CustomerCreateRequest.class);
        Customer domain = domainCustomer(ID, "098254785");
        CustomerResponse resp = mock(CustomerResponse.class);

        when(mapper.toDomain(req)).thenReturn(domain);
        when(service.create(domain)).thenReturn(Mono.just(domain));
        when(mapper.toResponse(domain)).thenReturn(resp);

        StepVerifier.create(controller.createCustomer(Mono.just(req), exchange("http://localhost/api/customers")))
                .assertNext(re -> {
                    assertEquals(201, re.getStatusCode().value());
                    assertSame(resp, re.getBody());
                })
                .verifyComplete();

        verify(mapper).toDomain(req);
        verify(service).create(domain);
        verify(mapper).toResponse(domain);
        verifyNoMoreInteractions(service, mapper);
    }

    @Test
    void createCustomer_error_shouldPropagateError() {
        CustomerCreateRequest req = mock(CustomerCreateRequest.class);
        Customer domain = domainCustomer(ID, "098254785");

        when(mapper.toDomain(req)).thenReturn(domain);
        when(service.create(domain)).thenReturn(Mono.error(new RuntimeException("boom")));

        StepVerifier.create(controller.createCustomer(Mono.just(req), exchange("http://localhost/api/customers")))
                .expectErrorMessage("boom")
                .verify();

        verify(mapper).toDomain(req);
        verify(service).create(domain);
        verifyNoMoreInteractions(service, mapper);
    }

    @Test
    void getCustomer_success_shouldReturn200() {
        Customer domain = domainCustomer(ID, "098254785");
        CustomerResponse resp = mock(CustomerResponse.class);

        when(service.getById(ID)).thenReturn(Mono.just(domain));
        when(mapper.toResponse(domain)).thenReturn(resp);

        StepVerifier.create(controller.getCustomer(ID, exchange("http://localhost/api/customers/" + ID)))
                .assertNext(re -> {
                    assertEquals(200, re.getStatusCode().value());
                    assertSame(resp, re.getBody());
                })
                .verifyComplete();

        verify(service).getById(ID);
        verify(mapper).toResponse(domain);
        verifyNoMoreInteractions(service, mapper);
    }

    @Test
    void getCustomer_error_shouldPropagateError() {
        when(service.getById(ID)).thenReturn(Mono.error(new RuntimeException("boom")));

        StepVerifier.create(controller.getCustomer(ID, exchange("http://localhost/api/customers/" + ID)))
                .expectErrorMessage("boom")
                .verify();

        verify(service).getById(ID);
        verifyNoMoreInteractions(service, mapper);
    }

    @Test
    void updateCustomer_success_shouldReturn200() {
        CustomerUpdateRequest req = mock(CustomerUpdateRequest.class);
        Customer patch = Customer.builder().build();
        Customer updated = domainCustomer(ID, "NEW-ID");
        CustomerResponse resp = mock(CustomerResponse.class);

        when(mapper.toPartial(req)).thenReturn(patch);
        when(service.update(ID, patch)).thenReturn(Mono.just(updated));
        when(mapper.toResponse(updated)).thenReturn(resp);

        StepVerifier.create(controller.updateCustomer(ID, Mono.just(req), exchange("http://localhost/api/customers/" + ID)))
                .assertNext(re -> {
                    assertEquals(200, re.getStatusCode().value());
                    assertSame(resp, re.getBody());
                })
                .verifyComplete();

        verify(mapper).toPartial(req);
        verify(service).update(ID, patch);
        verify(mapper).toResponse(updated);
        verifyNoMoreInteractions(service, mapper);
    }

    @Test
    void updateCustomer_error_shouldPropagateError() {
        CustomerUpdateRequest req = mock(CustomerUpdateRequest.class);
        Customer patch = Customer.builder().build();

        when(mapper.toPartial(req)).thenReturn(patch);
        when(service.update(ID, patch)).thenReturn(Mono.error(new RuntimeException("boom")));

        StepVerifier.create(controller.updateCustomer(ID, Mono.just(req), exchange("http://localhost/api/customers/" + ID)))
                .expectErrorMessage("boom")
                .verify();

        verify(mapper).toPartial(req);
        verify(service).update(ID, patch);
        verifyNoMoreInteractions(service, mapper);
    }

    @Test
    void deleteCustomer_success_shouldReturn200WithBody() {
        when(service.delete(ID)).thenReturn(Mono.empty());

        StepVerifier.create(controller.deleteCustomer(ID, exchange("http://localhost/api/customers/" + ID)))
                .assertNext(re -> {
                    assertEquals(200, re.getStatusCode().value());
                    assertNotNull(re.getBody());
                    assertEquals(ID.toString(), re.getBody().getId());
                    assertEquals("Customer deleted successfully", re.getBody().getMessage());
                    assertNotNull(re.getBody().getDeletedAt());
                })
                .verifyComplete();

        verify(service).delete(ID);
        verifyNoMoreInteractions(service, mapper);
    }

    @Test
    void deleteCustomer_error_shouldPropagateError() {
        when(service.delete(ID)).thenReturn(Mono.error(new RuntimeException("boom")));

        StepVerifier.create(controller.deleteCustomer(ID, exchange("http://localhost/api/customers/" + ID)))
                .expectErrorMessage("boom")
                .verify();

        verify(service).delete(ID);
        verifyNoMoreInteractions(service, mapper);
    }

    @Test
    void listCustomers_success_whenNoPrevNoNext_shouldNotIncludeLinkHeader() {
        Customer c1 = domainCustomer(UUID.randomUUID(), "ID-1");
        CustomerResponse r1 = mock(CustomerResponse.class);

        when(service.listPage(null, null, null, 0, 50))
                .thenReturn(Mono.just(new PageResult<>(Flux.just(c1), 0, 50, 1L, 1)));
        when(mapper.toResponse(c1)).thenReturn(r1);

        ServerWebExchange ex = exchange("http://localhost/api/customers");

        StepVerifier.create(controller.listCustomers(null, null, null, null, null, ex))
                .assertNext(re -> {
                    assertEquals(200, re.getStatusCode().value());
                    assertEquals("1", re.getHeaders().getFirst("X-Total-Count"));
                    assertNull(re.getHeaders().getFirst("Link"));

                    Flux<CustomerResponse> body = re.getBody();
                    assertNotNull(body);

                    StepVerifier.create(body)
                            .expectNext(r1)
                            .verifyComplete();
                })
                .verifyComplete();

        verify(service).listPage(null, null, null, 0, 50);
        verify(mapper).toResponse(c1);
        verifyNoMoreInteractions(service, mapper);
    }

    @Test
    void listCustomers_success_whenPrevAndNext_shouldIncludeLinkHeader() {
        Customer c1 = domainCustomer(UUID.randomUUID(), "ID-1");
        CustomerResponse r1 = mock(CustomerResponse.class);

        when(service.listPage(true, "abc", "jose", 2, 10))
                .thenReturn(Mono.just(new PageResult<>(Flux.just(c1), 2, 10, 100L, 5)));
        when(mapper.toResponse(c1)).thenReturn(r1);

        ServerWebExchange ex = exchange("http://localhost/api/customers?state=true&identification=abc&name=jose&page=2&size=10");

        StepVerifier.create(controller.listCustomers(2, 10, true, "abc", "jose", ex))
                .assertNext(re -> {
                    assertEquals(200, re.getStatusCode().value());
                    assertEquals("100", re.getHeaders().getFirst("X-Total-Count"));

                    String link = re.getHeaders().getFirst("Link");
                    assertNotNull(link);
                    assertTrue(link.contains("rel=\"prev\""));
                    assertTrue(link.contains("rel=\"next\""));
                    assertTrue(link.contains("page=1"));
                    assertTrue(link.contains("page=3"));
                    assertTrue(link.contains("size=10"));

                    Flux<CustomerResponse> body = re.getBody();
                    assertNotNull(body);

                    StepVerifier.create(body)
                            .expectNext(r1)
                            .verifyComplete();
                })
                .verifyComplete();

        verify(service).listPage(true, "abc", "jose", 2, 10);
        verify(mapper).toResponse(c1);
        verifyNoMoreInteractions(service, mapper);
    }

    @Test
    void listCustomers_error_shouldPropagateError() {
        when(service.listPage(false, "id", "name", 0, 1))
                .thenReturn(Mono.error(new RuntimeException("boom")));

        ServerWebExchange ex = exchange("http://localhost/api/customers?page=0&size=1");

        StepVerifier.create(controller.listCustomers(0, 1, false, "id", "name", ex))
                .expectErrorMessage("boom")
                .verify();

        verify(service).listPage(false, "id", "name", 0, 1);
        verifyNoMoreInteractions(service, mapper);
    }
}
