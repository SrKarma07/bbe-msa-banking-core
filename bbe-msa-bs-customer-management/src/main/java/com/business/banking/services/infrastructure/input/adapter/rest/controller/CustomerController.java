package com.business.banking.services.infrastructure.input.adapter.rest.controller;

import com.business.banking.services.application.input.port.CustomerServicePort;
import com.business.banking.services.infrastructure.input.adapter.rest.api.CustomersApi;
import com.business.banking.services.infrastructure.input.adapter.rest.dto.CustomerCreateRequest;
import com.business.banking.services.infrastructure.input.adapter.rest.dto.CustomerResponse;
import com.business.banking.services.infrastructure.input.adapter.rest.dto.CustomerUpdateRequest;
import com.business.banking.services.infrastructure.input.adapter.rest.dto.DeleteResponse;
import com.business.banking.services.infrastructure.input.adapter.rest.mapper.CustomerControllerMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CustomerController implements CustomersApi {

    private final CustomerServicePort service;
    private final CustomerControllerMapper mapper;

    @Override
    public Mono<ResponseEntity<CustomerResponse>> createCustomer(
            Mono<CustomerCreateRequest> customerCreateRequest,
            ServerWebExchange exchange
    ) {
        log.info("|-> [controller] createCustomer");

        return customerCreateRequest
                .map(mapper::toDomain)
                .flatMap(service::create)
                .map(mapper::toResponse)
                .map(body -> ResponseEntity.status(201).body(body))
                .doOnSuccess(r -> log.info("<-| [controller] createCustomer finished successfully"))
                .doOnError(e -> log.error("<-| [controller] createCustomer finished with error: {}", e.getMessage()));
    }

    @Override
    public Mono<ResponseEntity<CustomerResponse>> getCustomer(UUID id, ServerWebExchange exchange) {
        log.info("|-> [controller] getCustomer id={}", id);

        return service.getById(id)
                .map(mapper::toResponse)
                .map(ResponseEntity::ok)
                .doOnSuccess(r -> log.info("<-| [controller] getCustomer finished successfully"))
                .doOnError(e -> log.error("<-| [controller] getCustomer finished with error: {}", e.getMessage()));
    }

    @Override
    public Mono<ResponseEntity<CustomerResponse>> updateCustomer(
            UUID id,
            Mono<CustomerUpdateRequest> customerUpdateRequest,
            ServerWebExchange exchange
    ) {
        log.info("|-> [controller] updateCustomer id={}", id);

        return customerUpdateRequest
                .map(mapper::toPartial)
                .flatMap(patch -> service.update(id, patch))
                .map(mapper::toResponse)
                .map(ResponseEntity::ok)
                .doOnSuccess(r -> log.info("<-| [controller] updateCustomer finished successfully"))
                .doOnError(e -> log.error("<-| [controller] updateCustomer finished with error: {}", e.getMessage()));
    }

    @Override
    public Mono<ResponseEntity<DeleteResponse>> deleteCustomer(UUID id, ServerWebExchange exchange) {
        log.info("|-> [controller] deleteCustomer id={}", id);

        return service.delete(id)
                .thenReturn(ResponseEntity.ok(
                        new DeleteResponse()
                                .id(id.toString())
                                .message("Customer deleted successfully")
                                .deletedAt(OffsetDateTime.now())
                ))
                .doOnSuccess(r -> log.info("<-| [controller] deleteCustomer finished successfully"))
                .doOnError(e -> log.error("<-| [controller] deleteCustomer finished with error: {}", e.getMessage()));
    }

    @Override
    public Mono<ResponseEntity<Flux<CustomerResponse>>> listCustomers(
            Integer page,
            Integer size,
            Boolean state,
            String identification,
            String name,
            ServerWebExchange exchange
    ) {
        int safePage = (page == null) ? 0 : Math.max(0, page);
        int safeSize = (size == null) ? 50 : Math.min(Math.max(size, 1), 200);

        log.info("|-> [controller] listCustomers page={} size={} state={} identification={} name={}",
                safePage, safeSize, state, identification, name);

        return service.listPage(state, identification, name, safePage, safeSize)
                .map(pr -> {
                    Flux<CustomerResponse> body = pr.items().map(mapper::toResponse);

                    String link = PaginationLinks.from(exchange.getRequest().getURI(), pr).asHeader();

                    ResponseEntity.BodyBuilder builder = ResponseEntity.ok()
                            .header("X-Total-Count", String.valueOf(pr.totalItems()));

                    if (link != null && !link.isBlank()) {
                        builder.header("Link", link);
                    }

                    return builder.body(body);
                })
                .doOnSuccess(r -> log.info("<-| [controller] listCustomers finished successfully"))
                .doOnError(e -> log.error("<-| [controller] listCustomers finished with error: {}", e.getMessage()));
    }
}
