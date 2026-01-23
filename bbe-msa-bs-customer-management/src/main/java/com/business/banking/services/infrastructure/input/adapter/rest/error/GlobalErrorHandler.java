package com.business.banking.services.infrastructure.input.adapter.rest.error;

import com.business.banking.services.domain.exception.DomainErrorCode;
import com.business.banking.services.domain.exception.DomainException;
import com.business.banking.services.domain.exception.DuplicateIdentificationException;
import com.business.banking.services.domain.exception.InvalidRequestException;
import com.business.banking.services.domain.exception.NotFoundException;
import com.business.banking.services.infrastructure.input.adapter.rest.dto.Problem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.codec.DecodingException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebInputException;
import org.springframework.web.bind.support.WebExchangeBindException;

import jakarta.validation.ConstraintViolationException;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalErrorHandler {

    private static final MediaType PROBLEM_JSON = MediaType.valueOf("application/problem+json");

    private static Problem buildProblem(HttpStatus status, DomainErrorCode code, String title, String detail, URI instance, Map<String, Object> attrs) {
        Problem p = new Problem();
        p.setStatus(status.value());
        p.setTitle(title != null ? title : status.getReasonPhrase());
        p.setDetail(detail);
        p.setType(URI.create("urn:error:customer:" + code.name()));
        if (instance != null) p.setInstance(instance);

        if (attrs != null && !attrs.isEmpty()) {
            p.putAdditionalProperty("attributes", attrs);
        }
        p.putAdditionalProperty("errorCode", code.name());
        return p;
    }

    private static Mono<org.springframework.http.ResponseEntity<Problem>> respond(HttpStatus status, Problem body) {
        return Mono.just(
                org.springframework.http.ResponseEntity.status(status)
                        .contentType(PROBLEM_JSON)
                        .body(body)
        );
    }

    @ExceptionHandler(NotFoundException.class)
    public Mono<org.springframework.http.ResponseEntity<Problem>> handleNotFound(NotFoundException ex) {
        Problem p = buildProblem(
                HttpStatus.NOT_FOUND,
                ex.getErrorCode(),
                HttpStatus.NOT_FOUND.getReasonPhrase(),
                ex.getMessage(),
                null,
                ex.getAttributes()
        );
        return respond(HttpStatus.NOT_FOUND, p);
    }

    @ExceptionHandler(DuplicateIdentificationException.class)
    public Mono<org.springframework.http.ResponseEntity<Problem>> handleDuplicate(DuplicateIdentificationException ex) {
        Problem p = buildProblem(
                HttpStatus.CONFLICT,
                ex.getErrorCode(),
                HttpStatus.CONFLICT.getReasonPhrase(),
                ex.getMessage(),
                null,
                ex.getAttributes()
        );
        return respond(HttpStatus.CONFLICT, p);
    }

    @ExceptionHandler(InvalidRequestException.class)
    public Mono<org.springframework.http.ResponseEntity<Problem>> handleBadRequest(InvalidRequestException ex) {
        Problem p = buildProblem(
                HttpStatus.BAD_REQUEST,
                ex.getErrorCode(),
                "Validation error",
                ex.getMessage(),
                null,
                ex.getAttributes()
        );
        return respond(HttpStatus.BAD_REQUEST, p);
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<org.springframework.http.ResponseEntity<Problem>> handleBind(WebExchangeBindException ex) {
        List<Map<String, Object>> errors = ex.getFieldErrors().stream()
                .map(fe -> Map.<String, Object>of(
                        "field", fe.getField(),
                        "message", fe.getDefaultMessage(),
                        "rejectedValue", fe.getRejectedValue()
                ))
                .toList();

        Problem p = buildProblem(
                HttpStatus.BAD_REQUEST,
                DomainErrorCode.INVALID_REQUEST,
                "Validation error",
                "Invalid request payload or parameters",
                null,
                Map.of("errors", errors)
        );
        return respond(HttpStatus.BAD_REQUEST, p);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public Mono<org.springframework.http.ResponseEntity<Problem>> handleConstraint(ConstraintViolationException ex) {
        List<Map<String, Object>> violations = ex.getConstraintViolations().stream()
                .map(v -> Map.<String, Object>of(
                        "field", v.getPropertyPath().toString(),
                        "message", v.getMessage(),
                        "rejectedValue", v.getInvalidValue()
                ))
                .toList();

        Problem p = buildProblem(
                HttpStatus.BAD_REQUEST,
                DomainErrorCode.INVALID_REQUEST,
                "Constraint violation",
                "Invalid request parameters",
                null,
                Map.of("errors", violations)
        );
        return respond(HttpStatus.BAD_REQUEST, p);
    }

    @ExceptionHandler({ServerWebInputException.class, DecodingException.class, DateTimeParseException.class})
    public Mono<org.springframework.http.ResponseEntity<Problem>> handleInputErrors(Exception ex) {
        String detail;
        if (ex instanceof ServerWebInputException swe) {
            detail = (swe.getReason() != null) ? swe.getReason() : "Invalid request input";
        } else if (ex instanceof DecodingException) {
            detail = "Malformed or unreadable request body";
        } else if (ex instanceof DateTimeParseException) {
            detail = "Invalid date format (expected ISO-8601)";
        } else {
            detail = "Invalid request input";
        }

        Problem p = buildProblem(
                HttpStatus.BAD_REQUEST,
                DomainErrorCode.INVALID_REQUEST,
                "Bad Request",
                detail,
                null,
                null
        );
        return respond(HttpStatus.BAD_REQUEST, p);
    }

    @ExceptionHandler({DataIntegrityViolationException.class, DuplicateKeyException.class})
    public Mono<org.springframework.http.ResponseEntity<Problem>> handleIntegrity(Exception ex) {
        String mostSpecific = null;

        if (ex instanceof DataIntegrityViolationException dive) {
            mostSpecific = (dive.getMostSpecificCause() != null) ? dive.getMostSpecificCause().getMessage() : dive.getMessage();
        } else if (ex instanceof DuplicateKeyException dke) {
            mostSpecific = (dke.getMostSpecificCause() != null) ? dke.getMostSpecificCause().getMessage() : dke.getMessage();
        }

        Problem p = buildProblem(
                HttpStatus.CONFLICT,
                DomainErrorCode.DATA_CONFLICT,
                "Data conflict",
                mostSpecific != null ? ("Data integrity violation: " + mostSpecific) : "Data integrity violation",
                null,
                mostSpecific != null ? Map.of("cause", mostSpecific) : null
        );
        return respond(HttpStatus.CONFLICT, p);
    }

    @ExceptionHandler({ResponseStatusException.class, ErrorResponseException.class})
    public Mono<org.springframework.http.ResponseEntity<Problem>> handleSpringStatus(Exception ex) {
        HttpStatus status;

        if (ex instanceof ResponseStatusException rse) {
            status = HttpStatus.valueOf(rse.getStatusCode().value());
        } else if (ex instanceof ErrorResponseException ere) {
            status = HttpStatus.valueOf(ere.getStatusCode().value());
        } else {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        Problem p = buildProblem(
                status,
                DomainErrorCode.UNEXPECTED_ERROR,
                status.getReasonPhrase(),
                ex.getMessage(),
                null,
                null
        );
        return respond(status, p);
    }

    @ExceptionHandler(DomainException.class)
    public Mono<org.springframework.http.ResponseEntity<Problem>> handleOtherDomain(DomainException ex) {
        HttpStatus status = HttpStatus.BAD_REQUEST;

        if (ex.getErrorCode() == DomainErrorCode.CUSTOMER_NOT_FOUND) status = HttpStatus.NOT_FOUND;
        if (ex.getErrorCode() == DomainErrorCode.DUPLICATE_IDENTIFICATION) status = HttpStatus.CONFLICT;
        if (ex.getErrorCode() == DomainErrorCode.DATA_CONFLICT) status = HttpStatus.CONFLICT;

        Problem p = buildProblem(
                status,
                ex.getErrorCode(),
                status.getReasonPhrase(),
                ex.getMessage(),
                null,
                ex.getAttributes()
        );
        return respond(status, p);
    }

    @ExceptionHandler(Exception.class)
    public Mono<org.springframework.http.ResponseEntity<Problem>> handleGeneric(Exception ex) {
        log.error("[error] unexpected", ex);

        Problem p = buildProblem(
                HttpStatus.INTERNAL_SERVER_ERROR,
                DomainErrorCode.UNEXPECTED_ERROR,
                "Unexpected Error",
                ex.getMessage(),
                null,
                null
        );
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, p);
    }
}
