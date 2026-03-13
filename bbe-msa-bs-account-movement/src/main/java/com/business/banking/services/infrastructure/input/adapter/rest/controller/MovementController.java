package com.business.banking.services.infrastructure.input.adapter.rest.controller;

import com.business.banking.services.application.input.port.MovementServicePort;
import com.business.banking.services.infrastructure.input.adapter.rest.api.MovementsApi;
import com.business.banking.services.infrastructure.input.adapter.rest.dto.DeleteResponse;
import com.business.banking.services.infrastructure.input.adapter.rest.dto.MovementCreateRequest;
import com.business.banking.services.infrastructure.input.adapter.rest.dto.MovementResponse;
import com.business.banking.services.infrastructure.input.adapter.rest.dto.MovementUpdateRequest;
import com.business.banking.services.infrastructure.input.adapter.rest.dto.TransferCreateRequest;
import com.business.banking.services.infrastructure.input.adapter.rest.dto.TransferResponse;
import com.business.banking.services.infrastructure.input.adapter.rest.mapper.MovementControllerMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MovementController implements MovementsApi {

    private final MovementServicePort service;
    private final MovementControllerMapper mapper;

    @Override
    public Mono<ResponseEntity<MovementResponse>> createMovement(
            Mono<MovementCreateRequest> movementCreateRequest,
            String idempotencyKey,
            ServerWebExchange exchange
    ) {
        return movementCreateRequest
                .map(mapper::toDomain)
                .flatMap(req -> service.create(req, idempotencyKey))
                .map(mapper::toResponse)
                .map(r -> ResponseEntity.status(org.springframework.http.HttpStatus.CREATED).body(r));
    }

    @Override
    public Mono<ResponseEntity<TransferResponse>> createTransfer(
            Mono<TransferCreateRequest> transferCreateRequest,
            String idempotencyKey,
            ServerWebExchange exchange
    ) {
        return transferCreateRequest
                .map(mapper::toDomain)
                .flatMap(req -> service.transfer(req, idempotencyKey))
                .map(mapper::toResponse)
                .map(r -> ResponseEntity.status(org.springframework.http.HttpStatus.CREATED).body(r));
    }

    @Override
    public Mono<ResponseEntity<Flux<MovementResponse>>> listMovements(
            Integer page,
            Integer size,
            String accountNumber,
            LocalDate dateFrom,
            LocalDate dateTo,
            ServerWebExchange exchange
    ) {
        int safePage = page != null ? page : 0;
        int safeSize = size != null ? size : 50;

        return service.listPage(accountNumber, dateFrom, dateTo, safePage, safeSize)
                .map(pr -> {
                    var headers = new org.springframework.http.HttpHeaders();
                    headers.add("X-Total-Count", String.valueOf(pr.total()));
                    headers.add("Link", PaginationLinks.build(exchange, safePage, safeSize, pr.total()));

                    Flux<MovementResponse> body = pr.items().map(mapper::toResponse);
                    return ResponseEntity.ok().headers(headers).body(body);
                });
    }

    @Override
    public Mono<ResponseEntity<MovementResponse>> getMovement(UUID id, ServerWebExchange exchange) {
        return service.getById(id)
                .map(mapper::toResponse)
                .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<MovementResponse>> updateMovement(
            UUID id,
            Mono<MovementUpdateRequest> movementUpdateRequest,
            ServerWebExchange exchange
    ) {
        return movementUpdateRequest
                .map(mapper::toPatch)
                .flatMap(patch -> service.update(id, patch))
                .map(mapper::toResponse)
                .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<DeleteResponse>> deleteMovement(UUID id, ServerWebExchange exchange) {
        return service.delete(id)
                .thenReturn(new DeleteResponse()
                        .id(id.toString())
                        .message("Movement deleted successfully")
                        .deletedAt(OffsetDateTime.now(ZoneOffset.UTC))
                )
                .map(ResponseEntity::ok);
    }
}
