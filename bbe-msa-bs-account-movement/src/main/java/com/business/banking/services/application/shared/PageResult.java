package com.business.banking.services.application.shared;

import reactor.core.publisher.Flux;

public record PageResult<T>(Flux<T> items, long total) {
}
