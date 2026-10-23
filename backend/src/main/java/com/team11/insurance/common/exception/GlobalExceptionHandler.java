package com.team11.insurance.common.exception;

import com.team11.insurance.common.api.ErrorCode;
import com.team11.insurance.common.api.ErrorResponse;
import com.team11.insurance.common.logging.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.team11.insurance.applications.exception.ApplicationRejectedException;

/**
 * Centralized REST Error Handler according to SAD Section 4.4:
 * Transforms all exceptions into the standard error schema:
 * { timestamp, status, errorCode, message, details[], correlationId }
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles custom domain exceptions extending AppException.
     */
    @ExceptionHandler(AppException.class)
    public ResponseEntity<ErrorResponse> handleAppException(AppException ex, HttpServletRequest request) {
        String correlationId = resolveCorrelationId(request);
        ErrorCode ec = ex.getErrorCode();

        log.warn("Application business exception [{}]: {} (correlationId={})",
                ec.getCode(), ex.getMessage(), correlationId);

        ErrorResponse response = new ErrorResponse(
                ec.getHttpStatus().value(),
                ec.getCode(),
                ex.getMessage(),
                ex.getDetails(),
                correlationId
        );

        return ResponseEntity.status(ec.getHttpStatus()).body(response);
    }

    /**
     * Handles business underwriting rejection according to contract p3-contract.md §3.1:
     * { timestamp, status: 422, errorCode: "VALID-422", message: "Application rejected",
     *   details: [{ "reason": "<underwriting reason>" }], correlationId }
     */
    @ExceptionHandler(ApplicationRejectedException.class)
    public ResponseEntity<Map<String, Object>> handleApplicationRejected(
            ApplicationRejectedException ex, HttpServletRequest request) {
        String correlationId = resolveCorrelationId(request);
        log.warn("Application rejected (VALID-422): {} (correlationId={})", ex.getReason(), correlationId);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now());
        body.put("status", HttpStatus.UNPROCESSABLE_ENTITY.value());
        body.put("errorCode", ErrorCode.VALID_422.getCode());
        body.put("message", "Application rejected");
        body.put("details", List.of(Map.of("reason", ex.getReason())));
        body.put("correlationId", correlationId);

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(body);
    }

    /**
     * Handles Spring Bean Validation errors (@Valid @RequestBody).
     * Maps to VALID-422 with field-level details.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        String correlationId = resolveCorrelationId(request);

        List<String> details = new ArrayList<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            details.add(String.format("Field '%s': %s", fieldError.getField(), fieldError.getDefaultMessage()));
        }

        log.warn("Request validation failed (VALID-422) for URI {}: {} (correlationId={})",
                request.getRequestURI(), details, correlationId);

        ErrorResponse response = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                ErrorCode.VALID_422.getCode(),
                "Input failed validation or a business rule",
                details,
                correlationId
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handles JPA/Hibernate constraint violations (@Validated query/path params).
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest request) {
        String correlationId = resolveCorrelationId(request);

        List<String> details = new ArrayList<>();
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            details.add(String.format("%s: %s", violation.getPropertyPath(), violation.getMessage()));
        }

        log.warn("Constraint violation (VALID-422): {} (correlationId={})", details, correlationId);

        ErrorResponse response = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                ErrorCode.VALID_422.getCode(),
                "Input parameter constraint violation",
                details,
                correlationId
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handles malformed JSON payloads or missing request bodies.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        String correlationId = resolveCorrelationId(request);

        log.warn("Malformed HTTP request body (correlationId={}): {}", correlationId, ex.getMessage());

        ErrorResponse response = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                ErrorCode.VALID_422.getCode(),
                "Malformed JSON request body or incompatible data types",
                List.of("Request body could not be parsed"),
                correlationId
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handles missing required parameters or type mismatches.
     */
    @ExceptionHandler({
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class,
            IllegalArgumentException.class
    })
    public ResponseEntity<ErrorResponse> handleMissingOrMismatchParams(
            Exception ex, HttpServletRequest request) {
        String correlationId = resolveCorrelationId(request);

        ErrorResponse response = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                ErrorCode.VALID_422.getCode(),
                "Missing or invalid request parameter: " + ex.getMessage(),
                List.of(ex.getMessage()),
                correlationId
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handles 404 resource not found errors for static routes.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFound(
            NoResourceFoundException ex, HttpServletRequest request) {
        String correlationId = resolveCorrelationId(request);

        ErrorResponse response = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                ErrorCode.NOTFOUND_404.getCode(),
                "The requested resource was not found: " + request.getRequestURI(),
                List.of(),
                correlationId
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * Handles 405 Method Not Allowed.
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        String correlationId = resolveCorrelationId(request);

        ErrorResponse response = new ErrorResponse(
                HttpStatus.METHOD_NOT_ALLOWED.value(),
                ErrorCode.VALID_422.getCode(),
                "HTTP method " + ex.getMethod() + " is not supported for this endpoint",
                List.of(),
                correlationId
        );

        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(response);
    }

    /**
     * Fallback handler for all unexpected runtime errors (SYS-500).
     * Strictly avoids leaking internal details or stack traces to the client.
     */
    @ExceptionHandler(Throwable.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedError(Throwable ex, HttpServletRequest request) {
        String correlationId = resolveCorrelationId(request);

        // Internal details and stack trace are logged ONLY to server logs with correlationId
        log.error("Unhandled system error [SYS-500] (correlationId={}) for URI {}: {}",
                correlationId, request.getRequestURI(), ex.getMessage(), ex);

        // User sees only a generic message with the correlationId per SAD Section 4.4
        ErrorResponse response = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                ErrorCode.SYS_500.getCode(),
                "An unexpected internal error occurred. Please contact support quoting your correlation ID.",
                List.of(),
                correlationId
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    /**
     * Resolves the current correlationId from MDC or request headers, or creates a fallback UUID.
     */
    private String resolveCorrelationId(HttpServletRequest request) {
        String correlationId = MDC.get(CorrelationIdFilter.MDC_KEY);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = request.getHeader(CorrelationIdFilter.HEADER_NAME);
        }
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }
        return correlationId;
    }
}
