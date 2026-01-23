package com.business.banking.services.infrastructure.input.adapter.rest.controller;

import com.business.banking.services.application.input.port.MovementServicePort;
import com.business.banking.services.application.shared.PageResult;
import com.business.banking.services.domain.model.Movement;
import com.business.banking.services.infrastructure.input.adapter.rest.dto.DeleteResponse;
import com.business.banking.services.infrastructure.input.adapter.rest.dto.MovementCreateRequest;
import com.business.banking.services.infrastructure.input.adapter.rest.dto.MovementResponse;
import com.business.banking.services.infrastructure.input.adapter.rest.dto.MovementUpdateRequest;
import com.business.banking.services.infrastructure.input.adapter.rest.mapper.MovementControllerMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MovementControllerTest {

    @Mock
    private MovementServicePort service;

    @Mock
    private MovementControllerMapper mapper;

    @InjectMocks
    private MovementController controller;

    private ServerWebExchange exchange;

    @BeforeEach
    void setup() {
        exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/movements").build()
        );
    }

    @Test
    void createMovement_shouldMapRequest_callService_andReturn201() {
        MovementCreateRequest req = new MovementCreateRequest()
                .accountNumber("ACC-1")
                .date(LocalDate.parse("2025-02-10"))
                .type("Deposito")
                .value(new BigDecimal("25.00"))
                .detail("Cash deposit");

        Movement domain = Movement.builder()
                .accountNumber("ACC-1")
                .date(LocalDate.parse("2025-02-10"))
                .type("Deposito")
                .value(new BigDecimal("25.00"))
                .detail("Cash deposit")
                .build();

        Movement saved = domain.toBuilder()
                .id(UUID.randomUUID())
                .balance(new BigDecimal("125.00"))
                .build();

        MovementResponse response = new MovementResponse()
                .id(UUID.fromString(saved.getId().toString()))
                .accountNumber("ACC-1")
                .date(LocalDate.parse("2025-02-10"))
                .type("Deposito")
                .value(new BigDecimal("25.00"))
                .balance(new BigDecimal("125.00"));

        when(mapper.toDomain(req)).thenReturn(domain);
        when(service.create(domain, "KEY-1")).thenReturn(Mono.just(saved));
        when(mapper.toResponse(saved)).thenReturn(response);

        StepVerifier.create(controller.createMovement(Mono.just(req), "KEY-1", exchange))
                .assertNext(resp -> {
                    assertEquals(HttpStatus.CREATED, resp.getStatusCode());
                    assertNotNull(resp.getBody());
                    assertEquals("ACC-1", resp.getBody().getAccountNumber());
                    assertEquals("Deposito", resp.getBody().getType());
                    assertEquals(new BigDecimal("25.00"), resp.getBody().getValue());
                })
                .verifyComplete();

        verify(mapper).toDomain(req);
        verify(service).create(domain, "KEY-1");
        verify(mapper).toResponse(saved);
        verifyNoMoreInteractions(service, mapper);
    }

    @Test
    void listMovements_whenPageAndSizeNull_shouldUseDefaults_andReturnHeaders() {
        LocalDate from = LocalDate.parse("2025-02-01");
        LocalDate to = LocalDate.parse("2025-02-28");

        Movement m1 = Movement.builder().id(UUID.randomUUID()).accountNumber("ACC-1").type("Deposito").build();
        Movement m2 = Movement.builder().id(UUID.randomUUID()).accountNumber("ACC-1").type("Retiro").build();

        MovementResponse r1 = new MovementResponse().id(UUID.fromString(m1.getId().toString())).accountNumber("ACC-1").type("Deposito");
        MovementResponse r2 = new MovementResponse().id(UUID.fromString(m2.getId().toString())).accountNumber("ACC-1").type("Retiro");

        when(service.listPage("ACC-1", from, to, 0, 50))
                .thenReturn(Mono.just(new PageResult<>(Flux.just(m1, m2), 2L)));

        when(mapper.toResponse(m1)).thenReturn(r1);
        when(mapper.toResponse(m2)).thenReturn(r2);

        StepVerifier.create(controller.listMovements(null, null, "ACC-1", from, to, exchange))
                .assertNext(resp -> {
                    assertEquals(HttpStatus.OK, resp.getStatusCode());
                    assertEquals("2", resp.getHeaders().getFirst("X-Total-Count"));
                    assertNotNull(resp.getHeaders().getFirst("Link"));

                    Flux<MovementResponse> body = resp.getBody();
                    assertNotNull(body);

                    StepVerifier.create(body)
                            .assertNext(x -> assertEquals("Deposito", x.getType()))
                            .assertNext(x -> assertEquals("Retiro", x.getType()))
                            .verifyComplete();
                })
                .verifyComplete();

        verify(service).listPage("ACC-1", from, to, 0, 50);
        verify(mapper).toResponse(m1);
        verify(mapper).toResponse(m2);
        verifyNoMoreInteractions(service, mapper);
    }

    @Test
    void listMovements_whenPageAndSizeProvided_shouldUseProvidedValues() {
        LocalDate from = LocalDate.parse("2025-01-01");
        LocalDate to = LocalDate.parse("2025-01-31");

        Movement m1 = Movement.builder().id(UUID.randomUUID()).accountNumber("ACC-2").type("Deposito").build();
        MovementResponse r1 = new MovementResponse().id(UUID.fromString(m1.getId().toString())).accountNumber("ACC-2").type("Deposito");

        when(service.listPage("ACC-2", from, to, 2, 10))
                .thenReturn(Mono.just(new PageResult<>(Flux.just(m1), 11L)));
        when(mapper.toResponse(m1)).thenReturn(r1);

        ServerWebExchange ex2 = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/movements?page=2&size=10").build()
        );

        StepVerifier.create(controller.listMovements(2, 10, "ACC-2", from, to, ex2))
                .assertNext(resp -> {
                    assertEquals(HttpStatus.OK, resp.getStatusCode());
                    assertEquals("11", resp.getHeaders().getFirst("X-Total-Count"));
                    assertNotNull(resp.getHeaders().getFirst("Link"));

                    Flux<MovementResponse> body = resp.getBody();
                    assertNotNull(body);

                    StepVerifier.create(body)
                            .assertNext(x -> assertEquals("ACC-2", x.getAccountNumber()))
                            .verifyComplete();
                })
                .verifyComplete();

        verify(service).listPage("ACC-2", from, to, 2, 10);
        verify(mapper).toResponse(m1);
        verifyNoMoreInteractions(service, mapper);
    }

    @Test
    void getMovement_shouldCallService_mapResponse_andReturn200() {
        UUID id = UUID.randomUUID();

        Movement domain = Movement.builder().id(id).accountNumber("ACC-3").type("Deposito").build();

        MovementResponse response = mock(MovementResponse.class);
        when(response.getId()).thenReturn(id);

        when(service.getById(id)).thenReturn(Mono.just(domain));
        when(mapper.toResponse(domain)).thenReturn(response);

        StepVerifier.create(controller.getMovement(id, exchange))
                .assertNext(resp -> {
                    assertEquals(HttpStatus.OK, resp.getStatusCode());
                    assertNotNull(resp.getBody());
                    assertEquals(id, resp.getBody().getId());
                })
                .verifyComplete();

        verify(service).getById(id);
        verify(mapper).toResponse(domain);
        verifyNoMoreInteractions(service, mapper);
    }

    @Test
    void updateMovement_shouldMapPatch_callService_andReturn200() {
        UUID id = UUID.randomUUID();

        MovementUpdateRequest req = new MovementUpdateRequest();
        Movement patch = Movement.builder().detail("Fixed detail").build();

        Movement updated = Movement.builder()
                .id(id)
                .accountNumber("ACC-4")
                .type("Deposito")
                .detail("Fixed detail")
                .build();

        MovementResponse response = mock(MovementResponse.class);
        when(response.getId()).thenReturn(id);

        when(mapper.toPatch(req)).thenReturn(patch);
        when(service.update(id, patch)).thenReturn(Mono.just(updated));
        when(mapper.toResponse(updated)).thenReturn(response);

        StepVerifier.create(controller.updateMovement(id, Mono.just(req), exchange))
                .assertNext(resp -> {
                    assertEquals(HttpStatus.OK, resp.getStatusCode());
                    assertNotNull(resp.getBody());
                    assertEquals(id, resp.getBody().getId());
                })
                .verifyComplete();

        verify(mapper).toPatch(req);
        verify(service).update(id, patch);
        verify(mapper).toResponse(updated);
        verifyNoMoreInteractions(service, mapper);
    }


    @Test
    void deleteMovement_shouldCallService_andReturnDeleteResponse() {
        UUID id = UUID.randomUUID();

        when(service.delete(id)).thenReturn(Mono.empty());

        StepVerifier.create(controller.deleteMovement(id, exchange))
                .assertNext(resp -> {
                    assertEquals(HttpStatus.OK, resp.getStatusCode());

                    DeleteResponse body = resp.getBody();
                    assertNotNull(body);
                    assertEquals(id.toString(), body.getId());
                    assertEquals("Movement deleted successfully", body.getMessage());
                    assertNotNull(body.getDeletedAt());

                    OffsetDateTime now = OffsetDateTime.now();
                    assertTrue(body.getDeletedAt().isBefore(now.plusSeconds(5)));
                    assertTrue(body.getDeletedAt().isAfter(now.minusMinutes(1)));
                })
                .verifyComplete();

        verify(service).delete(id);
        verifyNoInteractions(mapper);
        verifyNoMoreInteractions(service);
    }
}
