package com.business.banking.services.infrastructure.output.repository.reactive;

import com.business.banking.services.infrastructure.output.repository.entity.AccountEntity;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.UUID;

public interface AccountR2dbcRepository extends ReactiveCrudRepository<AccountEntity, String> {

    Flux<AccountEntity> findAllByCustomerId(UUID customerId);

    Mono<Boolean> existsByNumber(String number);

    @Modifying
    @Query("UPDATE account SET balance = :newBalance WHERE number = :number")
    Mono<Integer> updateBalance(String number, BigDecimal newBalance);

    @Query("SELECT * FROM account ORDER BY number LIMIT :limit OFFSET :offset")
    Flux<AccountEntity> findPage(int offset, int limit);

    @Query("SELECT * FROM account WHERE customer_id = :customerId ORDER BY number LIMIT :limit OFFSET :offset")
    Flux<AccountEntity> findPageByCustomerId(UUID customerId, int offset, int limit);

    @Query("SELECT COUNT(1) FROM account")
    Mono<Long> countAll();

    @Query("SELECT COUNT(1) FROM account WHERE customer_id = :customerId")
    Mono<Long> countByCustomerId(UUID customerId);
}
