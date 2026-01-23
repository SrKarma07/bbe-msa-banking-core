package com.business.banking.services.infrastructure.input.adapter.rest.error;

import com.business.banking.services.domain.exception.DomainErrorCode;
import com.business.banking.services.domain.exception.DomainException;
import com.business.banking.services.domain.exception.DuplicateIdentificationException;
import com.business.banking.services.domain.exception.InvalidRequestException;
import com.business.banking.services.domain.exception.NotFoundException;
import com.business.banking.services.infrastructure.input.adapter.rest.dto.Problem;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.junit.jupiter.api.Test;
import org.springframework.core.codec.DecodingException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebInputException;
import reactor.test.StepVerifier;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URI;
import java.time.format.DateTimeParseException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GlobalErrorHandlerTest {

    private final GlobalErrorHandler handler = new GlobalErrorHandler();

    @Test
    void buildProblem_viaReflection_whenTitleNullAndInstanceAndAttrs_shouldFillDefaultsAndExtras() throws Exception {
        Method m = GlobalErrorHandler.class.getDeclaredMethod(
                "buildProblem",
                HttpStatus.class,
                DomainErrorCode.class,
                String.class,
                String.class,
                URI.class,
                Map.class
        );
        m.setAccessible(true);

        URI instance = URI.create("urn:test:instance");
        Map<String, Object> attrs = Map.of("k", "v");

        Problem p = (Problem) m.invoke(
                null,
                HttpStatus.BAD_REQUEST,
                DomainErrorCode.INVALID_REQUEST,
                null,
                "detail-x",
                instance,
                attrs
        );

        assertEquals(400, p.getStatus());
        assertEquals("Bad Request", p.getTitle());
        assertEquals("detail-x", p.getDetail());
        assertEquals(URI.create("urn:error:customer:INVALID_REQUEST"), p.getType());
        assertEquals(instance, p.getInstance());

        Map<String, Object> ap = additionalProps(p);
        assertEquals("INVALID_REQUEST", ap.get("errorCode"));

        Object attributes = ap.get("attributes");
        assertTrue(attributes instanceof Map);
        assertEquals("v", ((Map<?, ?>) attributes).get("k"));
    }

    @Test
    void handleNotFound_shouldReturn404_problemJsonAndErrorCodeAndAttributes() {
        NotFoundException ex = mock(NotFoundException.class);
        when(ex.getErrorCode()).thenReturn(DomainErrorCode.CUSTOMER_NOT_FOUND);
        when(ex.getMessage()).thenReturn("Customer not found");
        when(ex.getAttributes()).thenReturn(Map.of("id", "123"));

        StepVerifier.create(handler.handleNotFound(ex))
                .assertNext(re -> {
                    assertEquals(HttpStatus.NOT_FOUND, re.getStatusCode());
                    assertEquals(MediaType.valueOf("application/problem+json"), re.getHeaders().getContentType());

                    Problem p = re.getBody();
                    assertNotNull(p);
                    assertEquals(404, p.getStatus());
                    assertEquals("Not Found", p.getTitle());
                    assertEquals("Customer not found", p.getDetail());
                    assertEquals(URI.create("urn:error:customer:CUSTOMER_NOT_FOUND"), p.getType());

                    Map<String, Object> ap = additionalProps(p);
                    assertEquals("CUSTOMER_NOT_FOUND", ap.get("errorCode"));
                    assertEquals(Map.of("id", "123"), ap.get("attributes"));
                })
                .verifyComplete();
    }

    @Test
    void handleDuplicate_shouldReturn409_problemJsonAndErrorCodeAndAttributes() {
        DuplicateIdentificationException ex = mock(DuplicateIdentificationException.class);
        when(ex.getErrorCode()).thenReturn(DomainErrorCode.DUPLICATE_IDENTIFICATION);
        when(ex.getMessage()).thenReturn("Duplicate identification");
        when(ex.getAttributes()).thenReturn(Map.of("identification", "ABC"));

        StepVerifier.create(handler.handleDuplicate(ex))
                .assertNext(re -> {
                    assertEquals(HttpStatus.CONFLICT, re.getStatusCode());
                    assertEquals(MediaType.valueOf("application/problem+json"), re.getHeaders().getContentType());

                    Problem p = re.getBody();
                    assertNotNull(p);
                    assertEquals(409, p.getStatus());
                    assertEquals("Conflict", p.getTitle());
                    assertEquals("Duplicate identification", p.getDetail());
                    assertEquals(URI.create("urn:error:customer:DUPLICATE_IDENTIFICATION"), p.getType());

                    Map<String, Object> ap = additionalProps(p);
                    assertEquals("DUPLICATE_IDENTIFICATION", ap.get("errorCode"));
                    assertEquals(Map.of("identification", "ABC"), ap.get("attributes"));
                })
                .verifyComplete();
    }

    @Test
    void handleBadRequest_shouldReturn400_withValidationTitle() {
        InvalidRequestException ex = mock(InvalidRequestException.class);
        when(ex.getErrorCode()).thenReturn(DomainErrorCode.INVALID_REQUEST);
        when(ex.getMessage()).thenReturn("Missing required field");
        when(ex.getAttributes()).thenReturn(Map.of("field", "person.name"));

        StepVerifier.create(handler.handleBadRequest(ex))
                .assertNext(re -> {
                    assertEquals(HttpStatus.BAD_REQUEST, re.getStatusCode());
                    assertEquals(MediaType.valueOf("application/problem+json"), re.getHeaders().getContentType());

                    Problem p = re.getBody();
                    assertNotNull(p);
                    assertEquals(400, p.getStatus());
                    assertEquals("Validation error", p.getTitle());
                    assertEquals("Missing required field", p.getDetail());
                    assertEquals(URI.create("urn:error:customer:INVALID_REQUEST"), p.getType());

                    Map<String, Object> ap = additionalProps(p);
                    assertEquals("INVALID_REQUEST", ap.get("errorCode"));
                    assertEquals(Map.of("field", "person.name"), ap.get("attributes"));
                })
                .verifyComplete();
    }

    @Test
    void handleBind_shouldReturn400_withErrorsInAttributes() {
        WebExchangeBindException ex = mock(WebExchangeBindException.class);

        FieldError fe1 = new FieldError("obj", "name", "bad", false, null, null, "must not be blank");
        FieldError fe2 = new FieldError("obj", "age", -1, false, null, null, "must be >= 0");
        when(ex.getFieldErrors()).thenReturn(List.of(fe1, fe2));

        StepVerifier.create(handler.handleBind(ex))
                .assertNext(re -> {
                    assertEquals(HttpStatus.BAD_REQUEST, re.getStatusCode());

                    Problem p = re.getBody();
                    assertNotNull(p);
                    assertEquals(400, p.getStatus());
                    assertEquals("Validation error", p.getTitle());
                    assertEquals("Invalid request payload or parameters", p.getDetail());
                    assertEquals(URI.create("urn:error:customer:INVALID_REQUEST"), p.getType());

                    Map<String, Object> ap = additionalProps(p);
                    assertEquals("INVALID_REQUEST", ap.get("errorCode"));

                    Object attributes = ap.get("attributes");
                    assertTrue(attributes instanceof Map);
                    Object errorsObj = ((Map<?, ?>) attributes).get("errors");
                    assertTrue(errorsObj instanceof List);

                    List<?> errors = (List<?>) errorsObj;
                    assertEquals(2, errors.size());

                    Map<?, ?> e0 = (Map<?, ?>) errors.get(0);
                    assertEquals("name", e0.get("field"));
                    assertEquals("must not be blank", e0.get("message"));
                    assertEquals("bad", e0.get("rejectedValue"));

                    Map<?, ?> e1 = (Map<?, ?>) errors.get(1);
                    assertEquals("age", e1.get("field"));
                    assertEquals("must be >= 0", e1.get("message"));
                    assertEquals(-1, e1.get("rejectedValue"));
                })
                .verifyComplete();
    }

    @Test
    void handleConstraint_shouldReturn400_withViolationsInAttributes() {
        ConstraintViolation<?> v1 = mock(ConstraintViolation.class);
        Path p1 = mock(Path.class);

        when(p1.toString()).thenReturn("q.page");
        when(v1.getPropertyPath()).thenReturn(p1);
        when(v1.getMessage()).thenReturn("must be >= 0");
        when(v1.getInvalidValue()).thenReturn(-1);

        ConstraintViolation<?> v2 = mock(ConstraintViolation.class);
        Path p2 = mock(Path.class);

        when(p2.toString()).thenReturn("q.size");
        when(v2.getPropertyPath()).thenReturn(p2);
        when(v2.getMessage()).thenReturn("must be <= 200");
        when(v2.getInvalidValue()).thenReturn(500);

        ConstraintViolationException ex = new ConstraintViolationException(Set.of(v1, v2));

        StepVerifier.create(handler.handleConstraint(ex))
                .assertNext(re -> {
                    assertEquals(HttpStatus.BAD_REQUEST, re.getStatusCode());

                    Problem p = re.getBody();
                    assertNotNull(p);
                    assertEquals(400, p.getStatus());
                    assertEquals("Constraint violation", p.getTitle());
                    assertEquals("Invalid request parameters", p.getDetail());
                    assertEquals(URI.create("urn:error:customer:INVALID_REQUEST"), p.getType());

                    Map<String, Object> ap = additionalProps(p);
                    Object attributes = ap.get("attributes");
                    assertTrue(attributes instanceof Map);
                    Object errorsObj = ((Map<?, ?>) attributes).get("errors");
                    assertTrue(errorsObj instanceof List);

                    List<?> errors = (List<?>) errorsObj;
                    assertEquals(2, errors.size());
                })
                .verifyComplete();
    }

    @Test
    void handleInputErrors_whenServerWebInputExceptionWithReason_shouldUseReason() {
        ServerWebInputException ex = mock(ServerWebInputException.class);
        when(ex.getReason()).thenReturn("invalid param");

        StepVerifier.create(handler.handleInputErrors(ex))
                .assertNext(re -> {
                    assertEquals(HttpStatus.BAD_REQUEST, re.getStatusCode());
                    Problem p = re.getBody();
                    assertNotNull(p);
                    assertEquals("Bad Request", p.getTitle());
                    assertEquals("invalid param", p.getDetail());
                    assertEquals(URI.create("urn:error:customer:INVALID_REQUEST"), p.getType());
                })
                .verifyComplete();
    }

    @Test
    void handleInputErrors_whenServerWebInputExceptionWithoutReason_shouldUseDefaultDetail() {
        ServerWebInputException ex = mock(ServerWebInputException.class);
        when(ex.getReason()).thenReturn(null);

        StepVerifier.create(handler.handleInputErrors(ex))
                .assertNext(re -> {
                    assertEquals(HttpStatus.BAD_REQUEST, re.getStatusCode());
                    Problem p = re.getBody();
                    assertNotNull(p);
                    assertEquals("Invalid request input", p.getDetail());
                })
                .verifyComplete();
    }

    @Test
    void handleInputErrors_whenDecodingException_shouldUseMalformedDetail() {
        DecodingException ex = new DecodingException("decode");

        StepVerifier.create(handler.handleInputErrors(ex))
                .assertNext(re -> {
                    assertEquals(HttpStatus.BAD_REQUEST, re.getStatusCode());
                    Problem p = re.getBody();
                    assertNotNull(p);
                    assertEquals("Malformed or unreadable request body", p.getDetail());
                })
                .verifyComplete();
    }

    @Test
    void handleInputErrors_whenDateTimeParseException_shouldUseDateFormatDetail() {
        DateTimeParseException ex = new DateTimeParseException("bad", "x", 0);

        StepVerifier.create(handler.handleInputErrors(ex))
                .assertNext(re -> {
                    assertEquals(HttpStatus.BAD_REQUEST, re.getStatusCode());
                    Problem p = re.getBody();
                    assertNotNull(p);
                    assertEquals("Invalid date format (expected ISO-8601)", p.getDetail());
                })
                .verifyComplete();
    }

    @Test
    void handleIntegrity_whenDataIntegrityViolationWithMostSpecificCause_shouldIncludeCauseAttribute() {
        DataIntegrityViolationException ex = mock(DataIntegrityViolationException.class);
        Throwable most = new RuntimeException("unique constraint");
        when(ex.getMostSpecificCause()).thenReturn(most);
        when(ex.getMessage()).thenReturn("div");

        StepVerifier.create(handler.handleIntegrity(ex))
                .assertNext(re -> {
                    assertEquals(HttpStatus.CONFLICT, re.getStatusCode());
                    Problem p = re.getBody();
                    assertNotNull(p);
                    assertEquals(409, p.getStatus());
                    assertEquals("Data conflict", p.getTitle());
                    assertTrue(p.getDetail().contains("Data integrity violation: unique constraint"));

                    Map<String, Object> ap = additionalProps(p);
                    assertEquals("DATA_CONFLICT", ap.get("errorCode"));

                    Object attributes = ap.get("attributes");
                    assertTrue(attributes instanceof Map);
                    assertEquals("unique constraint", ((Map<?, ?>) attributes).get("cause"));
                })
                .verifyComplete();
    }

    @Test
    void handleSpringStatus_whenResponseStatusException_shouldMapStatusAndUnexpectedErrorCode() {
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.NOT_FOUND, "nope");

        StepVerifier.create(handler.handleSpringStatus(ex))
                .assertNext(re -> {
                    assertEquals(HttpStatus.NOT_FOUND, re.getStatusCode());
                    Problem p = re.getBody();
                    assertNotNull(p);
                    assertEquals(404, p.getStatus());
                    assertEquals("Not Found", p.getTitle());
                    assertNotNull(p.getDetail());

                    Map<String, Object> ap = additionalProps(p);
                    assertEquals("UNEXPECTED_ERROR", ap.get("errorCode"));
                })
                .verifyComplete();
    }

    @Test
    void handleSpringStatus_whenErrorResponseException_shouldMapStatusAndUnexpectedErrorCode() {
        ErrorResponseException ex = mock(ErrorResponseException.class);
        when(ex.getStatusCode()).thenReturn(HttpStatus.BAD_REQUEST);
        when(ex.getMessage()).thenReturn("bad");

        StepVerifier.create(handler.handleSpringStatus(ex))
                .assertNext(re -> {
                    assertEquals(HttpStatus.BAD_REQUEST, re.getStatusCode());
                    Problem p = re.getBody();
                    assertNotNull(p);
                    assertEquals(400, p.getStatus());
                    assertEquals("Bad Request", p.getTitle());
                    assertEquals("bad", p.getDetail());

                    Map<String, Object> ap = additionalProps(p);
                    assertEquals("UNEXPECTED_ERROR", ap.get("errorCode"));
                })
                .verifyComplete();
    }

    @Test
    void handleOtherDomain_whenCustomerNotFound_shouldReturn404() {
        DomainException ex = mock(DomainException.class);
        when(ex.getErrorCode()).thenReturn(DomainErrorCode.CUSTOMER_NOT_FOUND);
        when(ex.getMessage()).thenReturn("not found");
        when(ex.getAttributes()).thenReturn(Map.of("id", "x"));

        StepVerifier.create(handler.handleOtherDomain(ex))
                .assertNext(re -> {
                    assertEquals(HttpStatus.NOT_FOUND, re.getStatusCode());
                    Problem p = re.getBody();
                    assertNotNull(p);
                    assertEquals(404, p.getStatus());
                    assertEquals("Not Found", p.getTitle());

                    Map<String, Object> ap = additionalProps(p);
                    assertEquals("CUSTOMER_NOT_FOUND", ap.get("errorCode"));
                    assertEquals(Map.of("id", "x"), ap.get("attributes"));
                })
                .verifyComplete();
    }

    @Test
    void handleOtherDomain_whenDuplicateIdentification_shouldReturn409() {
        DomainException ex = mock(DomainException.class);
        when(ex.getErrorCode()).thenReturn(DomainErrorCode.DUPLICATE_IDENTIFICATION);
        when(ex.getMessage()).thenReturn("dup");
        when(ex.getAttributes()).thenReturn(Map.of("identification", "y"));

        StepVerifier.create(handler.handleOtherDomain(ex))
                .assertNext(re -> assertEquals(HttpStatus.CONFLICT, re.getStatusCode()))
                .verifyComplete();
    }

    @Test
    void handleOtherDomain_whenDataConflict_shouldReturn409() {
        DomainException ex = mock(DomainException.class);
        when(ex.getErrorCode()).thenReturn(DomainErrorCode.DATA_CONFLICT);
        when(ex.getMessage()).thenReturn("conflict");
        when(ex.getAttributes()).thenReturn(Map.of("cause", "z"));

        StepVerifier.create(handler.handleOtherDomain(ex))
                .assertNext(re -> assertEquals(HttpStatus.CONFLICT, re.getStatusCode()))
                .verifyComplete();
    }

    @Test
    void handleOtherDomain_whenOtherDomainError_shouldReturn400() {
        DomainException ex = mock(DomainException.class);
        when(ex.getErrorCode()).thenReturn(DomainErrorCode.INVALID_REQUEST);
        when(ex.getMessage()).thenReturn("bad");
        when(ex.getAttributes()).thenReturn(Map.of("a", "b"));

        StepVerifier.create(handler.handleOtherDomain(ex))
                .assertNext(re -> assertEquals(HttpStatus.BAD_REQUEST, re.getStatusCode()))
                .verifyComplete();
    }

    @Test
    void handleGeneric_shouldReturn500_unexpectedError() {
        Exception ex = new RuntimeException("boom");

        StepVerifier.create(handler.handleGeneric(ex))
                .assertNext(re -> {
                    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, re.getStatusCode());
                    assertEquals(MediaType.valueOf("application/problem+json"), re.getHeaders().getContentType());

                    Problem p = re.getBody();
                    assertNotNull(p);
                    assertEquals(500, p.getStatus());
                    assertEquals("Unexpected Error", p.getTitle());
                    assertEquals("boom", p.getDetail());

                    Map<String, Object> ap = additionalProps(p);
                    assertEquals("UNEXPECTED_ERROR", ap.get("errorCode"));
                })
                .verifyComplete();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> additionalProps(Problem p) {
        try {
            Method m = p.getClass().getMethod("getAdditionalProperties");
            Object v = m.invoke(p);
            if (v instanceof Map) return (Map<String, Object>) v;
        } catch (Exception ignored) {
        }

        try {
            Field f = p.getClass().getDeclaredField("additionalProperties");
            f.setAccessible(true);
            Object v = f.get(p);
            if (v instanceof Map) return (Map<String, Object>) v;
        } catch (Exception ignored) {
        }

        try {
            Method m = p.getClass().getMethod("getAdditionalProperty", String.class);
            Map<String, Object> map = new HashMap<>();
            for (String key : List.of("errorCode", "attributes")) {
                Object v = m.invoke(p, key);
                if (v != null) map.put(key, v);
            }
            return map;
        } catch (Exception ignored) {
        }

        return new HashMap<>();
    }
}
