package com.business.banking.services.infrastructure.input.adapter.rest.error;

import com.business.banking.services.domain.exception.*;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.codec.DecodingException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebInputException;
import reactor.test.StepVerifier;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GlobalErrorHandlerTest {

    private GlobalErrorHandler handler;

    @BeforeEach
    void setup() {
        handler = new GlobalErrorHandler();
    }

    private MockServerWebExchange exchange(String path) {
        return MockServerWebExchange.from(MockServerHttpRequest.get(path).build());
    }

    private MockServerWebExchange exchangeWithCorrelation(String path, String correlationId) {
        return MockServerWebExchange.from(
                MockServerHttpRequest.get(path)
                        .header("X-Correlation-Id", correlationId)
                        .build()
        );
    }

    @Test
    void handleNotFound_shouldReturn404_problemJson_andCorrelationId() {
        MockServerWebExchange ex = exchangeWithCorrelation("/api/test", "cid-1");
        NotFoundException nfe = NotFoundException.accountByNumber("ACC-1");

        StepVerifier.create(handler.handleNotFound(nfe, ex))
                .assertNext(resp -> {
                    assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
                    assertEquals(MediaType.APPLICATION_PROBLEM_JSON, resp.getHeaders().getContentType());

                    ProblemDetail pd = resp.getBody();
                    assertNotNull(pd);
                    assertEquals(404, pd.getStatus());
                    assertEquals("Not Found", pd.getTitle());
                    assertEquals("/api/test", pd.getInstance().toString());
                    assertEquals("cid-1", pd.getProperties().get("correlationId"));
                    assertNotNull(pd.getProperties().get("errorCode"));
                    assertNotNull(pd.getType());
                })
                .verifyComplete();
    }

    @Test
    void handleDuplicate_shouldReturn409_problemJson() {
        MockServerWebExchange ex = exchange("/api/dup");
        DuplicateAccountNumberException dae = new DuplicateAccountNumberException("ACC-99");

        StepVerifier.create(handler.handleDuplicate(dae, ex))
                .assertNext(resp -> {
                    assertEquals(HttpStatus.CONFLICT, resp.getStatusCode());
                    assertEquals(MediaType.APPLICATION_PROBLEM_JSON, resp.getHeaders().getContentType());

                    ProblemDetail pd = resp.getBody();
                    assertNotNull(pd);
                    assertEquals(409, pd.getStatus());
                    assertEquals("Conflict", pd.getTitle());
                    assertEquals("/api/dup", pd.getInstance().toString());
                    assertNotNull(pd.getProperties().get("errorCode"));
                    assertNotNull(pd.getType());
                })
                .verifyComplete();
    }

    @Test
    void handleUnprocessable_shouldReturn422() {
        MockServerWebExchange ex = exchange("/api/tx");
        InsufficientBalanceException ibe = new InsufficientBalanceException(
                "ACC-1",
                java.math.BigDecimal.TEN,
                java.math.BigDecimal.ONE.negate()
        );

        StepVerifier.create(handler.handleUnprocessable(ibe, ex))
                .assertNext(resp -> {
                    assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, resp.getStatusCode());
                    assertEquals(MediaType.APPLICATION_PROBLEM_JSON, resp.getHeaders().getContentType());

                    ProblemDetail pd = resp.getBody();
                    assertNotNull(pd);
                    assertEquals(422, pd.getStatus());
                    assertEquals("Unprocessable Entity", pd.getTitle());
                    assertEquals("/api/tx", pd.getInstance().toString());
                    assertNotNull(pd.getProperties().get("errorCode"));
                    assertNotNull(pd.getType());
                })
                .verifyComplete();
    }

    @Test
    void handleBadRequest_whenNormalInvalidRequest_shouldReturn400ProblemDetail() {
        InvalidRequestException ire = new InvalidRequestException("bad request", null);

        StepVerifier.create(handler.handleBadRequest(ire))
                .assertNext(pd -> {
                    assertNotNull(pd);
                    assertEquals(400, pd.getStatus());
                    assertEquals("bad request", pd.getDetail());
                    assertNotNull(pd.getProperties().get("errorCode"));
                    assertNotNull(pd.getType());
                })
                .verifyComplete();
    }

    @Test
    void handleBadRequest_whenIdempotencyConflict_shouldReturn409ProblemDetail() {
        InvalidRequestException ire = new InvalidRequestException(
                "Idempotency-Key was already used with a different request payload",
                null
        );

        StepVerifier.create(handler.handleBadRequest(ire))
                .assertNext(pd -> {
                    assertNotNull(pd);
                    assertEquals(409, pd.getStatus());
                    assertEquals("Idempotency-Key was already used with a different request payload", pd.getDetail());
                    assertNotNull(pd.getProperties().get("errorCode"));
                    assertNotNull(pd.getType());
                })
                .verifyComplete();
    }

    @Test
    void handleBind_shouldReturn400_withErrorsArray() {
        MockServerWebExchange ex = exchange("/api/bind");

        WebExchangeBindException we = mock(WebExchangeBindException.class);
        FieldError fe1 = new FieldError("obj", "field1", "bad");
        FieldError fe2 = new FieldError("obj", "field2", null, false, null, null, null);

        when(we.getFieldErrors()).thenReturn(List.of(fe1, fe2));

        StepVerifier.create(handler.handleBind(we, ex))
                .assertNext(resp -> {
                    assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
                    assertEquals(MediaType.APPLICATION_PROBLEM_JSON, resp.getHeaders().getContentType());

                    ProblemDetail pd = resp.getBody();
                    assertNotNull(pd);
                    assertEquals(400, pd.getStatus());
                    assertEquals("Bad Request", pd.getTitle());
                    assertEquals("Invalid request payload or parameters", pd.getDetail());
                    assertEquals("/api/bind", pd.getInstance().toString());

                    Object attrs = pd.getProperties().get("attributes");
                    assertNotNull(attrs);
                })
                .verifyComplete();
    }

    @Test
    void handleConstraint_shouldReturn400_withViolationsArray() {
        MockServerWebExchange ex = exchange("/api/constraint");

        @SuppressWarnings("unchecked")
        ConstraintViolation<Object> v1 = (ConstraintViolation<Object>) mock(ConstraintViolation.class);
        Path p1 = mock(Path.class);

        when(p1.toString()).thenReturn("fieldA");
        when(v1.getPropertyPath()).thenReturn(p1);
        when(v1.getMessage()).thenReturn("must not be null");
        when(v1.getInvalidValue()).thenReturn(null);

        ConstraintViolationException cve = new ConstraintViolationException(Set.of(v1));

        StepVerifier.create(handler.handleConstraint(cve, ex))
                .assertNext(resp -> {
                    assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
                    assertEquals(MediaType.APPLICATION_PROBLEM_JSON, resp.getHeaders().getContentType());

                    ProblemDetail pd = resp.getBody();
                    assertNotNull(pd);
                    assertEquals(400, pd.getStatus());
                    assertEquals("Bad Request", pd.getTitle());
                    assertEquals("Invalid request parameters", pd.getDetail());
                    assertEquals("/api/constraint", pd.getInstance().toString());
                    assertNotNull(pd.getProperties().get("attributes"));
                })
                .verifyComplete();
    }

    @Test
    void handleInputErrors_whenServerWebInputException_shouldUseReason() {
        MockServerWebExchange ex = exchange("/api/input");
        ServerWebInputException swe = new ServerWebInputException("bad input");

        StepVerifier.create(handler.handleInputErrors(swe, ex))
                .assertNext(resp -> {
                    assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());

                    ProblemDetail pd = resp.getBody();
                    assertNotNull(pd);
                    assertEquals(400, pd.getStatus());
                    assertEquals("Bad Request", pd.getTitle());
                    assertEquals("bad input", pd.getDetail());
                })
                .verifyComplete();
    }

    @Test
    void handleInputErrors_whenDecodingException_shouldUseMalformedDetail() {
        MockServerWebExchange ex = exchange("/api/dec");
        DecodingException de = new DecodingException("decode");

        StepVerifier.create(handler.handleInputErrors(de, ex))
                .assertNext(resp -> {
                    assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());

                    ProblemDetail pd = resp.getBody();
                    assertNotNull(pd);
                    assertEquals(400, pd.getStatus());
                    assertEquals("Malformed or unreadable request body", pd.getDetail());
                })
                .verifyComplete();
    }

    @Test
    void handleInputErrors_whenDateTimeParseException_shouldUseDateFormatDetail() {
        MockServerWebExchange ex = exchange("/api/date");
        DateTimeParseException dte = new DateTimeParseException("invalid", "x", 0);

        StepVerifier.create(handler.handleInputErrors(dte, ex))
                .assertNext(resp -> {
                    assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());

                    ProblemDetail pd = resp.getBody();
                    assertNotNull(pd);
                    assertEquals(400, pd.getStatus());
                    assertEquals("Invalid date format (expected ISO-8601 yyyy-MM-dd)", pd.getDetail());
                })
                .verifyComplete();
    }

    @Test
    void handleIntegrity_shouldReturn409_withCause() {
        MockServerWebExchange ex = exchange("/api/integrity");

        RuntimeException root = new RuntimeException("root-cause");
        DataIntegrityViolationException dive = new DataIntegrityViolationException("msg", root);

        StepVerifier.create(handler.handleIntegrity(dive, ex))
                .assertNext(resp -> {
                    assertEquals(HttpStatus.CONFLICT, resp.getStatusCode());

                    ProblemDetail pd = resp.getBody();
                    assertNotNull(pd);
                    assertEquals(409, pd.getStatus());
                    assertTrue(pd.getDetail().contains("Data integrity violation"));
                    assertNotNull(pd.getProperties().get("attributes"));
                })
                .verifyComplete();
    }

    @Test
    void handleDuplicateKey_shouldReturn409_withCause() {
        MockServerWebExchange ex = exchange("/api/dupkey");

        RuntimeException root = new RuntimeException("dup-root");
        DuplicateKeyException dke = new DuplicateKeyException("msg", root);

        StepVerifier.create(handler.handleDuplicateKey(dke, ex))
                .assertNext(resp -> {
                    assertEquals(HttpStatus.CONFLICT, resp.getStatusCode());

                    ProblemDetail pd = resp.getBody();
                    assertNotNull(pd);
                    assertEquals(409, pd.getStatus());
                    assertTrue(pd.getDetail().contains("Duplicate key detected"));
                    assertNotNull(pd.getProperties().get("attributes"));
                })
                .verifyComplete();
    }

    @Test
    void handleSpringStatus_whenResponseStatusException_shouldReturnWithStatusAndUnexpectedErrorCode() {
        MockServerWebExchange ex = exchange("/api/spring");
        ResponseStatusException rse = new ResponseStatusException(HttpStatus.BAD_REQUEST, "spring bad");

        StepVerifier.create(handler.handleSpringStatus(rse, ex))
                .assertNext(resp -> {
                    assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());

                    ProblemDetail pd = resp.getBody();
                    assertNotNull(pd);
                    assertEquals(400, pd.getStatus());
                    assertEquals("Bad Request", pd.getTitle());
                    assertEquals("HTTP Error", pd.getProperties().getOrDefault("title", "HTTP Error").toString());
                    assertNotNull(pd.getProperties().get("errorCode"));
                    assertNotNull(pd.getType());
                })
                .verifyComplete();
    }

    @Test
    void handleSpringStatus_whenErrorResponseException_shouldReturnWithStatusAndUnexpectedErrorCode() {
        MockServerWebExchange ex = exchange("/api/spring2");

        ProblemDetail base = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        ErrorResponseException ere = new ErrorResponseException(HttpStatus.CONFLICT, base, null);

        StepVerifier.create(handler.handleSpringStatus(ere, ex))
                .assertNext(resp -> {
                    assertEquals(HttpStatus.CONFLICT, resp.getStatusCode());

                    ProblemDetail pd = resp.getBody();
                    assertNotNull(pd);
                    assertEquals(409, pd.getStatus());
                    assertEquals("Conflict", pd.getTitle());
                    assertNotNull(pd.getProperties().get("errorCode"));
                    assertNotNull(pd.getType());
                })
                .verifyComplete();
    }


    @Test
    void handleOtherDomain_shouldReturn422() {
        MockServerWebExchange ex = exchange("/api/other");
        AccountClosedException ace = new AccountClosedException("ACC-1");

        StepVerifier.create(handler.handleOtherDomain(ace, ex))
                .assertNext(resp -> {
                    assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, resp.getStatusCode());

                    ProblemDetail pd = resp.getBody();
                    assertNotNull(pd);
                    assertEquals(422, pd.getStatus());
                    assertNotNull(pd.getProperties().get("errorCode"));
                    assertNotNull(pd.getType());
                })
                .verifyComplete();
    }

    @Test
    void handleGeneric_shouldReturn500_andSetInstance_andNoCorrelationWhenMissing() {
        MockServerWebExchange ex = exchange("/api/generic");
        RuntimeException boom = new RuntimeException("boom");

        StepVerifier.create(handler.handleGeneric(boom, ex))
                .assertNext(resp -> {
                    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, resp.getStatusCode());
                    assertEquals(MediaType.APPLICATION_PROBLEM_JSON, resp.getHeaders().getContentType());

                    ProblemDetail pd = resp.getBody();
                    assertNotNull(pd);
                    assertEquals(500, pd.getStatus());
                    assertEquals("Internal Server Error", pd.getTitle());
                    assertEquals("/api/generic", pd.getInstance().toString());
                    assertEquals("boom", pd.getDetail());
                    assertNotNull(pd.getProperties().get("errorCode"));
                    assertNotNull(pd.getType());
                    assertNull(pd.getProperties().get("correlationId"));
                })
                .verifyComplete();
    }

    @Test
    void handleGeneric_shouldIncludeCorrelationIdWhenPresent() {
        MockServerWebExchange ex = exchangeWithCorrelation("/api/generic2", "cid-xyz");
        RuntimeException boom = new RuntimeException("boom2");

        StepVerifier.create(handler.handleGeneric(boom, ex))
                .assertNext(resp -> {
                    ProblemDetail pd = resp.getBody();
                    assertNotNull(pd);
                    assertEquals("cid-xyz", pd.getProperties().get("correlationId"));
                })
                .verifyComplete();
    }
}
