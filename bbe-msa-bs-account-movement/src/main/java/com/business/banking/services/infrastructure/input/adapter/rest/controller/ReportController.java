package com.business.banking.services.infrastructure.input.adapter.rest.controller;

import com.business.banking.services.application.input.port.ReportServicePort;
import com.business.banking.services.domain.model.AccountStatement;
import com.business.banking.services.infrastructure.input.adapter.rest.api.ReportsApi;
import com.business.banking.services.infrastructure.input.adapter.rest.dto.AccountStatementResponse;
import com.business.banking.services.infrastructure.input.adapter.rest.mapper.ReportControllerMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ReportController implements ReportsApi {

    private final ReportServicePort service;
    private final ReportControllerMapper mapper;

    @Override
    public Mono<ResponseEntity<AccountStatementResponse>> getAccountStatement(
            UUID customerId,
            LocalDate dateFrom,
            LocalDate dateTo,
            ServerWebExchange exchange
    ) {
        log.info("|-> [report] getAccountStatement start customerId={} from={} to={}", customerId, dateFrom, dateTo);
        Mono<AccountStatement> reportMono = service.getAccountStatement(String.valueOf(customerId), dateFrom, dateTo);
        return reportMono
                .map(mapper::toResponse)
                .map(ResponseEntity::ok)
                .doOnSuccess(r -> log.info("<-| [report] getAccountStatement finished successfully"))
                .doOnError(e -> log.error("<-| [report] getAccountStatement finished with error: {}", e.getMessage()));
    }
}
