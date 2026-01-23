package com.business.banking.services.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.reactive.TransactionalOperator;

@Configuration
public class R2dbcConfig {
    @Bean
    public TransactionalOperator transactionalOperator(ReactiveTransactionManager rtm) {
        return TransactionalOperator.create(rtm);
    }
}
