package com.business.banking.services.infrastructure.input.adapter.rest.controller;

import com.business.banking.services.application.input.port.AccountServicePort;
import com.business.banking.services.infrastructure.input.adapter.rest.api.AccountsApi;
import com.business.banking.services.infrastructure.input.adapter.rest.dto.AccountCreateRequest;
import com.business.banking.services.infrastructure.input.adapter.rest.dto.AccountResponse;
import com.business.banking.services.infrastructure.input.adapter.rest.dto.AccountUpdateRequest;
import com.business.banking.services.infrastructure.input.adapter.rest.dto.DeleteResponse;
import com.business.banking.services.infrastructure.input.adapter.rest.mapper.AccountControllerMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AccountController implements AccountsApi {

    private final AccountServicePort service;
    private final AccountControllerMapper mapper;

    @Override
    public Mono<ResponseEntity<Flux<AccountResponse>>> listAccounts(Integer page, Integer size, UUID customerId, ServerWebExchange exchange) {
        int safePage = page != null ? page : 0;
        int safeSize = size != null ? size : 50;

        return service.listPage(customerId, safePage, safeSize)
                .map(pr -> {
                    var headers = new org.springframework.http.HttpHeaders();
                    headers.add("X-Total-Count", String.valueOf(pr.total()));
                    headers.add("Link", PaginationLinks.build(exchange, safePage, safeSize, pr.total()));
                    Flux<AccountResponse> body = pr.items().map(mapper::toResponse);
                    return ResponseEntity.ok().headers(headers).body(body);
                });
    }


    @Override
    public Mono<ResponseEntity<AccountResponse>> createAccount(Mono<AccountCreateRequest> accountCreateRequest, ServerWebExchange exchange) {
        return accountCreateRequest
                .map(mapper::toDomain)
                .flatMap(service::create)
                .map(mapper::toResponse)
                .map(body -> ResponseEntity.status(org.springframework.http.HttpStatus.CREATED).body(body));
    }

    @Override
    public Mono<ResponseEntity<AccountResponse>> getAccount(String number, ServerWebExchange exchange) {
        return service.getByNumber(number)
                .map(mapper::toResponse)
                .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<AccountResponse>> updateAccount(String number, Mono<AccountUpdateRequest> accountUpdateRequest, ServerWebExchange exchange) {
        return accountUpdateRequest
                .map(mapper::toPatch)
                .flatMap(p -> service.update(number, p))
                .map(mapper::toResponse)
                .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<DeleteResponse>> deleteAccount(String number, ServerWebExchange exchange) {
        return service.delete(number)
                .thenReturn(new DeleteResponse()
                        .id(number)
                        .message("Account deleted successfully")
                        .deletedAt(OffsetDateTime.now(ZoneOffset.UTC))
                )
                .map(ResponseEntity::ok);
    }

}
