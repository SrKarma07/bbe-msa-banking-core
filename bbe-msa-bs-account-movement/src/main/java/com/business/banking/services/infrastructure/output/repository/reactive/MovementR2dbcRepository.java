package com.business.banking.services.infrastructure.output.repository.reactive;

import com.business.banking.services.infrastructure.output.repository.entity.MovementEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.UUID;

public interface MovementR2dbcRepository extends ReactiveCrudRepository<MovementEntity, UUID> {

    Flux<MovementEntity> findAllByAccountNumberOrderByDateAsc(String accountNumber);

    Flux<MovementEntity> findAllByAccountNumberAndDateBetweenOrderByDateAsc(String accountNumber, LocalDate from, LocalDate to);

    Mono<MovementEntity> findFirstByIdempotencyKey(String idempotencyKey);

    @Query("SELECT * FROM movement ORDER BY date ASC, id ASC LIMIT :limit OFFSET :offset")
    Flux<com.business.banking.services.infrastructure.output.repository.entity.MovementEntity> findPage(int offset, int limit);

    @Query("SELECT * FROM movement WHERE account_number = :accountNumber ORDER BY date ASC, id ASC LIMIT :limit OFFSET :offset")
    Flux<com.business.banking.services.infrastructure.output.repository.entity.MovementEntity> findPageByAccount(String accountNumber, int offset, int limit);

    @Query("""
       SELECT * FROM movement
       WHERE account_number = :accountNumber
         AND date >= :from AND date <= :to
       ORDER BY date ASC, id ASC
       LIMIT :limit OFFSET :offset
       """)
    Flux<com.business.banking.services.infrastructure.output.repository.entity.MovementEntity> findPageByAccountAndDateRange(
            String accountNumber,
            LocalDate from,
            LocalDate to,
            int offset,
            int limit
    );

    @Query("SELECT COUNT(1) FROM movement")
    Mono<Long> countAll();

    @Query("SELECT COUNT(1) FROM movement WHERE account_number = :accountNumber")
    Mono<Long> countByAccount(String accountNumber);

    @Query("SELECT COUNT(1) FROM movement WHERE account_number = :accountNumber AND date >= :from AND date <= :to")
    Mono<Long> countByAccountAndDateRange(String accountNumber, LocalDate from, LocalDate to);

    @Query("""
   SELECT * FROM movement
   WHERE account_number IN (:accountNumbers)
     AND date >= :from AND date <= :to
   ORDER BY account_number ASC, date ASC, id ASC
   """)
    Flux<MovementEntity> findAllByAccountNumbersAndDateRange(
            java.util.Collection<String> accountNumbers,
            LocalDate from,
            LocalDate to
    );
}
