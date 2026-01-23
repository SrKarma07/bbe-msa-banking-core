package com.business.banking.services.infrastructure.output.repository.reactive;

import com.business.banking.services.infrastructure.output.repository.entity.PersonEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface PersonR2dbcRepository extends ReactiveCrudRepository<PersonEntity, Long> {

    @Query("SELECT EXISTS(SELECT 1 FROM person p WHERE p.identification = :identification)")
    Mono<Boolean> existsByIdentification(String identification);

    @Query("SELECT * FROM person p WHERE p.identification = :identification LIMIT 1")
    Mono<PersonEntity> findByIdentification(String identification);
}
