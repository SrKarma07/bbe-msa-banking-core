package com.business.banking.services.application.input.port;

import com.business.banking.services.domain.model.AccountStatement;
import reactor.core.publisher.Mono;

public interface ReportServicePort {
    Mono<AccountStatement> getAccountStatement(String customerId, java.time.LocalDate from, java.time.LocalDate to);

}
