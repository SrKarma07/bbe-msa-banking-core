package com.business.banking.services.application.input.port;

import com.business.banking.services.application.shared.PageResult;
import com.business.banking.services.domain.model.Account;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface AccountServicePort {

    Flux<Account> list(UUID customerId);

    Mono<PageResult<Account>> listPage(UUID customerId, int page, int size);

    Mono<Account> getByNumber(String accountNumber);

    Mono<Account> create(Account account);

    Mono<Account> update(String accountNumber, Account patch);

    Mono<Void> delete(String accountNumber);
}
