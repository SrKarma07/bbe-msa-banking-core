package com.business.banking.services.infrastructure.input.adapter.rest.error;

import com.business.banking.services.domain.exception.*;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.codec.DecodingException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.*;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalErrorHandler {

    private static final MediaType PROBLEM_JSON = MediaType.APPLICATION_PROBLEM_JSON;
    private static final String URN_PREFIX = "urn:error:bbe-msa-bs-account-movement:";
    private static final String CORRELATION_HEADER = "X-Correlation-Id";

    private static ResponseEntity<ProblemDetail> respond(ServerWebExchange exchange, HttpStatus status, ProblemDetail pd) {
        pd.setStatus(status.value());
        pd.setTitle(status.getReasonPhrase());
        pd.setInstance(URI.create(exchange.getRequest().getPath().value()));

        String correlationId = exchange.getRequest().getHeaders().getFirst(CORRELATION_HEADER);
        if (correlationId != null && !correlationId.isBlank()) {
            pd.setProperty("correlationId", correlationId);
        }

        return ResponseEntity.status(status).contentType(PROBLEM_JSON).body(pd);
    }

    private static ProblemDetail buildProblem(HttpStatus status, DomainException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(status);
        pd.setDetail(ex.getMessage());
        pd.setType(URI.create(URN_PREFIX + ex.getErrorCode().name()));
        pd.setProperty("errorCode", ex.getErrorCode().name());
        if (ex.getAttributes() != null && !ex.getAttributes().isEmpty()) {
            pd.setProperty("attributes", ex.getAttributes());
        }
        return pd;
    }

    private static ProblemDetail buildProblem(
            HttpStatus status,
            String title,
            String detail,
            DomainErrorCode code,
            Map<String, Object> attrs
    ) {
        ProblemDetail pd = ProblemDetail.forStatus(status);
        pd.setTitle(title);
        pd.setDetail(detail);
        pd.setType(URI.create(URN_PREFIX + code.name()));
        pd.setProperty("errorCode", code.name());
        if (attrs != null && !attrs.isEmpty()) pd.setProperty("attributes", attrs);
        return pd;
    }

    @ExceptionHandler(NotFoundException.class)
    public Mono<ResponseEntity<ProblemDetail>> handleNotFound(NotFoundException ex, ServerWebExchange exchange) {
        ProblemDetail pd = buildProblem(HttpStatus.NOT_FOUND, ex);
        return Mono.just(respond(exchange, HttpStatus.NOT_FOUND, pd));
    }

    @ExceptionHandler(DuplicateAccountNumberException.class)
    public Mono<ResponseEntity<ProblemDetail>> handleDuplicate(DuplicateAccountNumberException ex, ServerWebExchange exchange) {
        ProblemDetail pd = buildProblem(HttpStatus.CONFLICT, ex);
        return Mono.just(respond(exchange, HttpStatus.CONFLICT, pd));
    }

    @ExceptionHandler({InsufficientBalanceException.class, AccountClosedException.class})
    public Mono<ResponseEntity<ProblemDetail>> handleUnprocessable(DomainException ex, ServerWebExchange exchange) {
        ProblemDetail pd = buildProblem(HttpStatus.UNPROCESSABLE_ENTITY, ex);
        return Mono.just(respond(exchange, HttpStatus.UNPROCESSABLE_ENTITY, pd));
    }

    @ExceptionHandler(InvalidRequestException.class)
    public Mono<ProblemDetail> handleBadRequest(InvalidRequestException ex) {
        if (ex.getMessage() != null && ex.getMessage().startsWith("Idempotency-Key was already used")) {
            return Mono.just(buildProblem(HttpStatus.CONFLICT, ex));
        }
        return Mono.just(buildProblem(HttpStatus.BAD_REQUEST, ex));
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ResponseEntity<ProblemDetail>> handleBind(WebExchangeBindException ex, ServerWebExchange exchange) {
        List<Map<String, Object>> errors = ex.getFieldErrors().stream()
                .map(fe -> {
                    Map<String, Object> map = new java.util.HashMap<>();
                    map.put("field", fe.getField());
                    map.put("message", fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "Invalid value");
                    map.put("rejectedValue", fe.getRejectedValue() != null ? fe.getRejectedValue() : "null");
                    return map;
                })
                .toList();

        ProblemDetail pd = buildProblem(
                HttpStatus.BAD_REQUEST,
                "Validation error",
                "Invalid request payload or parameters",
                DomainErrorCode.INVALID_REQUEST,
                Map.of("errors", errors)
        );

        return Mono.just(respond(exchange, HttpStatus.BAD_REQUEST, pd));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public Mono<ResponseEntity<ProblemDetail>> handleConstraint(ConstraintViolationException ex, ServerWebExchange exchange) {
        List<Map<String, Object>> violations = ex.getConstraintViolations().stream()
                .map(v -> {
                    Map<String, Object> map = new java.util.HashMap<>();
                    map.put("field", v.getPropertyPath().toString());
                    map.put("message", v.getMessage() != null ? v.getMessage() : "Invalid value");
                    map.put("rejectedValue", v.getInvalidValue() != null ? v.getInvalidValue() : "null");
                    return map;
                })
                .toList();

        ProblemDetail pd = buildProblem(
                HttpStatus.BAD_REQUEST,
                "Constraint violation",
                "Invalid request parameters",
                DomainErrorCode.INVALID_REQUEST,
                Map.of("errors", violations)
        );

        return Mono.just(respond(exchange, HttpStatus.BAD_REQUEST, pd));
    }

    @ExceptionHandler({
            ServerWebInputException.class,
            DecodingException.class,
            DateTimeParseException.class
    })
    public Mono<ResponseEntity<ProblemDetail>> handleInputErrors(Exception ex, ServerWebExchange exchange) {
        String detail;

        if (ex instanceof ServerWebInputException swe) {
            detail = (swe.getReason() != null) ? swe.getReason() : "Invalid request input";
        } else if (ex instanceof DecodingException) {
            detail = "Malformed or unreadable request body";
        } else if (ex instanceof DateTimeParseException) {
            detail = "Invalid date format (expected ISO-8601 yyyy-MM-dd)";
        } else {
            detail = "Invalid request input";
        }

        ProblemDetail pd = buildProblem(
                HttpStatus.BAD_REQUEST,
                "Bad Request",
                detail,
                DomainErrorCode.INVALID_REQUEST,
                null
        );

        return Mono.just(respond(exchange, HttpStatus.BAD_REQUEST, pd));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public Mono<ResponseEntity<ProblemDetail>> handleIntegrity(DataIntegrityViolationException ex, ServerWebExchange exchange) {
        String cause = (ex.getMostSpecificCause() != null)
                ? ex.getMostSpecificCause().getMessage()
                : ex.getMessage();

        ProblemDetail pd = buildProblem(
                HttpStatus.CONFLICT,
                "Data conflict",
                "Data integrity violation: " + cause,
                DomainErrorCode.DATA_CONFLICT,
                Map.of("cause", cause)
        );

        return Mono.just(respond(exchange, HttpStatus.CONFLICT, pd));
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public Mono<ResponseEntity<ProblemDetail>> handleDuplicateKey(DuplicateKeyException ex, ServerWebExchange exchange) {
        String cause = (ex.getMostSpecificCause() != null)
                ? ex.getMostSpecificCause().getMessage()
                : ex.getMessage();

        ProblemDetail pd = buildProblem(
                HttpStatus.CONFLICT,
                "Data conflict",
                "Duplicate key detected: " + cause,
                DomainErrorCode.DATA_CONFLICT,
                Map.of("cause", cause)
        );

        return Mono.just(respond(exchange, HttpStatus.CONFLICT, pd));
    }

    @ExceptionHandler({ResponseStatusException.class, ErrorResponseException.class})
    public Mono<ResponseEntity<ProblemDetail>> handleSpringStatus(Exception ex, ServerWebExchange exchange) {
        HttpStatusCode code =
                (ex instanceof ResponseStatusException rse) ? rse.getStatusCode()
                        : (ex instanceof ErrorResponseException ere) ? ere.getStatusCode()
                        : HttpStatus.INTERNAL_SERVER_ERROR;

        HttpStatus status = HttpStatus.resolve(code.value()) != null
                ? HttpStatus.valueOf(code.value())
                : HttpStatus.INTERNAL_SERVER_ERROR;

        ProblemDetail pd = ProblemDetail.forStatus(code);
        pd.setTitle("HTTP Error");
        pd.setDetail(ex.getMessage());
        pd.setType(URI.create(URN_PREFIX + DomainErrorCode.UNEXPECTED_ERROR.name()));
        pd.setProperty("errorCode", DomainErrorCode.UNEXPECTED_ERROR.name());

        return Mono.just(respond(exchange, status, pd));
    }

    @ExceptionHandler(DomainException.class)
    public Mono<ResponseEntity<ProblemDetail>> handleOtherDomain(DomainException ex, ServerWebExchange exchange) {
        ProblemDetail pd = buildProblem(HttpStatus.UNPROCESSABLE_ENTITY, ex);
        return Mono.just(respond(exchange, HttpStatus.UNPROCESSABLE_ENTITY, pd));
    }

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ProblemDetail>> handleGeneric(Exception ex, ServerWebExchange exchange) {
        log.error("[error] unexpected", ex);

        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        pd.setTitle("Unexpected Error");
        pd.setDetail(ex.getMessage());
        pd.setType(URI.create(URN_PREFIX + DomainErrorCode.UNEXPECTED_ERROR.name()));
        pd.setProperty("errorCode", DomainErrorCode.UNEXPECTED_ERROR.name());

        return Mono.just(respond(exchange, HttpStatus.INTERNAL_SERVER_ERROR, pd));
    }
}
