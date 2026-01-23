package com.business.banking.services.infrastructure.output.repository.reactive;

import com.business.banking.services.infrastructure.output.repository.entity.CustomerEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface CustomerR2dbcRepository extends ReactiveCrudRepository<CustomerEntity, UUID> {

    @Query("""
  SELECT
    c.id            AS customer_id,
    c.password      AS password,
    c.state         AS state,
    p.person_id     AS person_id,
    p.name          AS name,
    p.gender        AS gender,
    p.age           AS age,
    p.identification AS identification,
    p.address       AS address,
    p.phone_number  AS phone_number
  FROM customer c
  JOIN person p ON p.person_id = c.person_id
  WHERE c.id = :id
""")
    Mono<CustomerPersonRow> findRowById(UUID id);


    @Query("""
  SELECT
    c.id            AS customer_id,
    c.password      AS password,
    c.state         AS state,
    p.person_id     AS person_id,
    p.name          AS name,
    p.gender        AS gender,
    p.age           AS age,
    p.identification AS identification,
    p.address       AS address,
    p.phone_number  AS phone_number
  FROM customer c
  JOIN person p ON p.person_id = c.person_id
  WHERE (:state IS NULL OR c.state = :state)
    AND (:identification IS NULL OR p.identification = :identification)
    AND (:name IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%')))
  ORDER BY p.person_id
  LIMIT :limit OFFSET :offset
""")
    Flux<CustomerPersonRow> findPageRows(Boolean state, String identification, String name, long offset, long limit);



    @Query("""
            SELECT COUNT(*)
            FROM customer c
            JOIN person p ON p.person_id = c.person_id
            WHERE (:state IS NULL OR c.state = :state)
              AND (:identification IS NULL OR p.identification = :identification)
              AND (:name IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%')))
            """)
    Mono<Long> countRows(Boolean state, String identification, String name);

    @Query("""
            SELECT EXISTS(
              SELECT 1
              FROM customer c
              JOIN person p ON p.person_id = c.person_id
              WHERE p.identification = :identification
                AND c.id <> :customerId
            )
            """)
    Mono<Boolean> existsByIdentificationAndDifferentCustomer(UUID customerId, String identification);
}
