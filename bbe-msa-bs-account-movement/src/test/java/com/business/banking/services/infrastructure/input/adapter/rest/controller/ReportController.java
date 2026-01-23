package com.business.banking.services.infrastructure.input.adapter.rest.controller;

import com.business.banking.services.application.input.port.ReportServicePort;
import com.business.banking.services.domain.model.AccountStatement;
import com.business.banking.services.infrastructure.input.adapter.rest.dto.AccountStatementResponse;
import com.business.banking.services.infrastructure.input.adapter.rest.mapper.ReportControllerMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportControllerTest {

    @Mock
    private ReportServicePort service;

    @Mock
    private ReportControllerMapper mapper;

    @InjectMocks
    private ReportController controller;

    private MockServerWebExchange exchange() {
        return MockServerWebExchange.from(MockServerHttpRequest.get("/api/reports").build());
    }

    @Test
    void getAccountStatement_shouldCallService_mapResponse_andReturn200() {
        UUID customerId = UUID.randomUUID();
        LocalDate from = LocalDate.of(2025, 1, 1);
        LocalDate to = LocalDate.of(2025, 1, 31);

        AccountStatement statement = AccountStatement.builder()
                .customerId(customerId.toString())
                .dateFrom(from)
                .dateTo(to)
                .build();

        AccountStatementResponse response = mock(AccountStatementResponse.class);

        when(service.getAccountStatement(customerId.toString(), from, to)).thenReturn(Mono.just(statement));
        when(mapper.toResponse(statement)).thenReturn(response);

        StepVerifier.create(controller.getAccountStatement(customerId, from, to, exchange()))
                .assertNext(resp -> {
                    assertEquals(HttpStatus.OK, resp.getStatusCode());
                    assertNotNull(resp.getBody());
                    assertSame(response, resp.getBody());
                })
                .verifyComplete();

        verify(service).getAccountStatement(customerId.toString(), from, to);
        verify(mapper).toResponse(statement);
        verifyNoMoreInteractions(service, mapper);
    }

    @Test
    void getAccountStatement_whenServiceErrors_shouldPropagateError_andNotCallMapper() {
        UUID customerId = UUID.randomUUID();
        LocalDate from = LocalDate.of(2025, 2, 1);
        LocalDate to = LocalDate.of(2025, 2, 10);

        RuntimeException boom = new RuntimeException("boom");
        when(service.getAccountStatement(customerId.toString(), from, to)).thenReturn(Mono.error(boom));

        StepVerifier.create(controller.getAccountStatement(customerId, from, to, exchange()))
                .expectErrorSatisfies(ex -> assertSame(boom, ex))
                .verify();

        verify(service).getAccountStatement(customerId.toString(), from, to);
        verifyNoInteractions(mapper);
        verifyNoMoreInteractions(service);
    }
}
