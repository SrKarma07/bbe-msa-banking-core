package com.business.banking.services.infrastructure.input.adapter.rest.filter;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
public class CorrelationIdWebFilter implements WebFilter {

    public static final String CORRELATION_HEADER = "X-Correlation-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String correlationId = exchange.getRequest().getHeaders().getFirst(CORRELATION_HEADER);

        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();

            ServerHttpRequest mutatedRequest = exchange.getRequest()
                    .mutate()
                    .header(CORRELATION_HEADER, correlationId)
                    .build();

            exchange = exchange.mutate().request(mutatedRequest).build();
        }

        exchange.getResponse().getHeaders().set(CORRELATION_HEADER, correlationId);

        return chain.filter(exchange);
    }
}
