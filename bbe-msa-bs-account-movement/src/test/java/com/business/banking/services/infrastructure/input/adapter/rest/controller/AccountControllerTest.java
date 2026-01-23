package com.business.banking.services.infrastructure.input.adapter.rest.controller;

import com.business.banking.services.application.input.port.AccountServicePort;
import com.business.banking.services.application.shared.PageResult;
import com.business.banking.services.domain.model.Account;
import com.business.banking.services.infrastructure.input.adapter.rest.dto.AccountCreateRequest;
import com.business.banking.services.infrastructure.input.adapter.rest.dto.AccountResponse;
import com.business.banking.services.infrastructure.input.adapter.rest.dto.AccountUpdateRequest;
import com.business.banking.services.infrastructure.input.adapter.rest.dto.DeleteResponse;
import com.business.banking.services.infrastructure.input.adapter.rest.mapper.AccountControllerMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountControllerTest {

    @Mock
    private AccountServicePort service;

    @Mock
    private AccountControllerMapper mapper;

    @InjectMocks
    private AccountController controller;

    private ServerWebExchange exchange;

    @BeforeEach
    void setup() {
        exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/accounts").build()
        );
    }

    @Test
    void listAccounts_whenPageAndSizeNull_shouldUseDefaults_andReturnHeaders() {
        UUID customerId = UUID.randomUUID();

        Account a1 = Account.builder().number("001").build();
        Account a2 = Account.builder().number("002").build();

        AccountResponse r1 = new AccountResponse().accountNumber("001");
        AccountResponse r2 = new AccountResponse().accountNumber("002");

        when(service.listPage(customerId, 0, 50))
                .thenReturn(Mono.just(new PageResult<>(Flux.just(a1, a2), 2L)));
        when(mapper.toResponse(a1)).thenReturn(r1);
        when(mapper.toResponse(a2)).thenReturn(r2);

        StepVerifier.create(controller.listAccounts(null, null, customerId, exchange))
                .assertNext(resp -> {
                    assertEquals(HttpStatus.OK, resp.getStatusCode());
                    assertEquals("2", resp.getHeaders().getFirst("X-Total-Count"));
                    assertNotNull(resp.getHeaders().getFirst("Link"));

                    Flux<AccountResponse> body = resp.getBody();
                    assertNotNull(body);

                    StepVerifier.create(body)
                            .assertNext(ar -> assertEquals("001", ar.getAccountNumber()))
                            .assertNext(ar -> assertEquals("002", ar.getAccountNumber()))
                            .verifyComplete();
                })
                .verifyComplete();

        verify(service).listPage(customerId, 0, 50);
        verify(mapper).toResponse(a1);
        verify(mapper).toResponse(a2);
        verifyNoMoreInteractions(service, mapper);
    }

    @Test
    void listAccounts_whenPageAndSizeProvided_shouldUseProvidedValues() {
        UUID customerId = UUID.randomUUID();

        Account a1 = Account.builder().number("003").build();
        AccountResponse r1 = new AccountResponse().accountNumber("003");

        when(service.listPage(customerId, 2, 10))
                .thenReturn(Mono.just(new PageResult<>(Flux.just(a1), 11L)));
        when(mapper.toResponse(a1)).thenReturn(r1);

        ServerWebExchange ex2 = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/accounts?page=2&size=10").build()
        );

        StepVerifier.create(controller.listAccounts(2, 10, customerId, ex2))
                .assertNext(resp -> {
                    assertEquals(HttpStatus.OK, resp.getStatusCode());
                    assertEquals("11", resp.getHeaders().getFirst("X-Total-Count"));
                    assertNotNull(resp.getHeaders().getFirst("Link"));

                    Flux<AccountResponse> body = resp.getBody();
                    assertNotNull(body);

                    StepVerifier.create(body)
                            .assertNext(ar -> assertEquals("003", ar.getAccountNumber()))
                            .verifyComplete();
                })
                .verifyComplete();

        verify(service).listPage(customerId, 2, 10);
        verify(mapper).toResponse(a1);
        verifyNoMoreInteractions(service, mapper);
    }

    @Test
    void createAccount_shouldMapCreateRequest_callService_andReturn201() {
        String customerId = "ce069cbb-fd30-4f39-8ca6-903d0217d951";

        AccountCreateRequest req = new AccountCreateRequest()
                .accountNumber("ACC-1")
                .type("Ahorro")
                .initialBalance(new BigDecimal("10.00"))
                .state(true)
                .customerId(UUID.fromString(customerId));

        Account domain = Account.builder().number("ACC-1").build();
        Account saved = Account.builder().number("ACC-1").build();

        AccountResponse response = new AccountResponse().accountNumber("ACC-1");

        when(mapper.toDomain(req)).thenReturn(domain);
        when(service.create(domain)).thenReturn(Mono.just(saved));
        when(mapper.toResponse(saved)).thenReturn(response);

        StepVerifier.create(controller.createAccount(Mono.just(req), exchange))
                .assertNext(resp -> {
                    assertEquals(HttpStatus.CREATED, resp.getStatusCode());
                    assertNotNull(resp.getBody());
                    assertEquals("ACC-1", resp.getBody().getAccountNumber());
                })
                .verifyComplete();

        verify(mapper).toDomain(req);
        verify(service).create(domain);
        verify(mapper).toResponse(saved);
        verifyNoMoreInteractions(service, mapper);
    }

    @Test
    void getAccount_shouldCallService_mapResponse_andReturn200() {
        Account domain = Account.builder().number("ACC-2").build();
        AccountResponse response = new AccountResponse().accountNumber("ACC-2");

        when(service.getByNumber("ACC-2")).thenReturn(Mono.just(domain));
        when(mapper.toResponse(domain)).thenReturn(response);

        StepVerifier.create(controller.getAccount("ACC-2", exchange))
                .assertNext(resp -> {
                    assertEquals(HttpStatus.OK, resp.getStatusCode());
                    assertNotNull(resp.getBody());
                    assertEquals("ACC-2", resp.getBody().getAccountNumber());
                })
                .verifyComplete();

        verify(service).getByNumber("ACC-2");
        verify(mapper).toResponse(domain);
        verifyNoMoreInteractions(service, mapper);
    }

    @Test
    void updateAccount_shouldMapPatch_callService_andReturn200() {
        AccountUpdateRequest req = new AccountUpdateRequest()
                .type("Corriente")
                .state(false);

        Account patch = Account.builder().type("Corriente").state(false).build();
        Account updated = Account.builder().number("ACC-3").type("Corriente").state(false).build();

        AccountResponse response = new AccountResponse().accountNumber("ACC-3");

        when(mapper.toPatch(req)).thenReturn(patch);
        when(service.update("ACC-3", patch)).thenReturn(Mono.just(updated));
        when(mapper.toResponse(updated)).thenReturn(response);

        StepVerifier.create(controller.updateAccount("ACC-3", Mono.just(req), exchange))
                .assertNext(resp -> {
                    assertEquals(HttpStatus.OK, resp.getStatusCode());
                    assertNotNull(resp.getBody());
                    assertEquals("ACC-3", resp.getBody().getAccountNumber());
                })
                .verifyComplete();

        verify(mapper).toPatch(req);
        verify(service).update("ACC-3", patch);
        verify(mapper).toResponse(updated);
        verifyNoMoreInteractions(service, mapper);
    }

    @Test
    void deleteAccount_shouldCallService_andReturnDeleteResponse() {
        when(service.delete("ACC-4")).thenReturn(Mono.empty());

        StepVerifier.create(controller.deleteAccount("ACC-4", exchange))
                .assertNext(resp -> {
                    assertEquals(HttpStatus.OK, resp.getStatusCode());

                    DeleteResponse body = resp.getBody();
                    assertNotNull(body);
                    assertEquals("ACC-4", body.getId());
                    assertEquals("Account deleted successfully", body.getMessage());
                    assertNotNull(body.getDeletedAt());

                    OffsetDateTime now = OffsetDateTime.now();
                    assertTrue(body.getDeletedAt().isBefore(now.plusSeconds(5)));
                    assertTrue(body.getDeletedAt().isAfter(now.minusMinutes(1)));
                })
                .verifyComplete();

        verify(service).delete("ACC-4");
        verifyNoInteractions(mapper);
        verifyNoMoreInteractions(service);
    }
}
