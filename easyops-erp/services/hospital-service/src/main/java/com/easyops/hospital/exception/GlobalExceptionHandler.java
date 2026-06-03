package com.easyops.hospital.exception;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import com.easyops.hospital.dto.response.DuplicatePatientResponse;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * FR-P1.4a / FR-P1.10 — business rules that yield HTTP 422 with optional {@code code} for clients.
     */
    @ExceptionHandler(UnprocessableEntityException.class)
    public ResponseEntity<Map<String, Object>> handleUnprocessableEntity(UnprocessableEntityException ex) {
        log.warn("Unprocessable entity (422): code={} message={}", ex.getCode(), ex.getMessage());
        Map<String, Object> response = new HashMap<>();
        response.put("error", ex.getMessage());
        response.put("status", HttpStatus.UNPROCESSABLE_ENTITY.value());
        if (ex.getCode() != null && !ex.getCode().isBlank()) {
            response.put("code", ex.getCode());
        }
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(response);
    }

    @ExceptionHandler(DuplicatePatientConflictException.class)
    public ResponseEntity<DuplicatePatientResponse> handleDuplicatePatientConflict(DuplicatePatientConflictException ex) {
        log.info("Duplicate patient registration blocked: {} match(es)", 
            ex.getDuplicatePatientResponse().getMatches() != null 
                ? ex.getDuplicatePatientResponse().getMatches().size() : 0);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getDuplicatePatientResponse());
    }

    /**
     * Must run before {@link #handleRuntimeException(RuntimeException)} so NOT_FOUND and other
     * status codes are not turned into generic 400 responses.
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatusException(ResponseStatusException ex) {
        log.warn("ResponseStatusException: {} - {}", ex.getStatusCode(), ex.getReason());
        Map<String, Object> response = new HashMap<>();
        String reason = ex.getReason();
        response.put("error", reason);
        response.put("message", reason);
        response.put("status", ex.getStatusCode().value());
        return ResponseEntity.status(ex.getStatusCode()).body(response);
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<Map<String, Object>> handleMissingRequestHeader(MissingRequestHeaderException ex) {
        log.warn("Missing request header: {}", ex.getMessage());
        Map<String, Object> response = new HashMap<>();
        String message = "X-User-Id".equals(ex.getHeaderName())
            ? "Your session is missing user identity. Please sign out and sign in again."
            : "Required request header is missing: " + ex.getHeaderName();
        response.put("error", message);
        response.put("message", message);
        response.put("status", HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    
    /**
     * Database / persistence errors: always 500 — these are server-side failures,
     * not bad input from the client.
     */
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Map<String, Object>> handleDataAccessException(DataAccessException ex) {
        if (isMultipleBagFetchException(ex)) {
            log.warn("Data access exception (multiple bag fetch): {}", ex.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("error", "Please retry the request. A temporary prescription data loading conflict was detected.");
            response.put("status", HttpStatus.CONFLICT.value());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }
        log.error("Data access exception: ", ex);
        Map<String, Object> response = new HashMap<>();
        response.put("error", "A database error occurred. Please try again or contact support.");
        response.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    /**
     * Malformed JSON or unreadable request body → 400.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        log.warn("Unreadable request body: {}", ex.getMessage());
        Throwable cause = ex.getMostSpecificCause();
        String detail = (cause != null && cause.getMessage() != null) ? cause.getMessage() : ex.getMessage();
        Map<String, Object> response = new HashMap<>();
        response.put("error", "Invalid request body" + (detail != null ? ": " + detail : ""));
        response.put("status", HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Unsupported HTTP method on a valid endpoint path (e.g. POST on PUT-only route) → 405.
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        log.warn("HTTP method not supported: {}", ex.getMessage());
        Map<String, Object> response = new HashMap<>();
        response.put("error", "Method not allowed for this endpoint");
        response.put("status", HttpStatus.METHOD_NOT_ALLOWED.value());
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(response);
    }

    /**
     * Explicit bad-argument errors thrown from service logic (e.g. missing required field).
     * Keep as 400.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Illegal argument: {}", ex.getMessage());
        Map<String, Object> response = new HashMap<>();
        response.put("error", ex.getMessage());
        response.put("status", HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Workspace optimistic-lock failure → 409.
     * The controller handles this directly; this handler is a safety net for cases where
     * the exception propagates past the controller (e.g. via AOP or filter chains).
     */
    @ExceptionHandler(WorkspaceConflictException.class)
    public ResponseEntity<Map<String, Object>> handleWorkspaceConflict(WorkspaceConflictException ex) {
        log.warn("EP workspace conflict (global handler): {}", ex.getMessage());
        Map<String, Object> response = new HashMap<>();
        response.put("conflict", true);
        response.put("serverVersion", ex.getServerVersion());
        response.put("message", ex.getMessage());
        response.put("status", HttpStatus.CONFLICT.value());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .header("X-EP-Workspace-Version", String.valueOf(ex.getServerVersion()))
                .body(response);
    }

    /**
     * Catch-all for other RuntimeExceptions (unexpected server errors) → 500.
     * Previously this returned 400, which masked server-side failures as client errors.
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex) {
        log.error("Unexpected runtime exception: ", ex);
        Map<String, Object> response = new HashMap<>();
        response.put("error", "An unexpected error occurred. Please try again or contact support.");
        response.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            if (error instanceof FieldError fieldError) {
                errors.put(fieldError.getField(), fieldError.getDefaultMessage());
            } else {
                String key = error.getObjectName();
                if (key == null || key.isBlank()) {
                    key = "_global";
                }
                errors.put(key, error.getDefaultMessage());
            }
        });

        Map<String, Object> response = new HashMap<>();
        response.put("errors", errors);
        response.put("status", HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Bean-validation method-level constraint violations (e.g. @Valid on @RequestParam,
     * or @Validated service-level constraints) — return 400 with violation details.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolation(ConstraintViolationException ex) {
        log.warn("Constraint violation: {}", ex.getMessage());
        Map<String, String> errors = new HashMap<>();
        ex.getConstraintViolations().forEach(v -> {
            String path = v.getPropertyPath().toString();
            String field = path.contains(".") ? path.substring(path.lastIndexOf('.') + 1) : path;
            errors.put(field, v.getMessage());
        });
        Map<String, Object> response = new HashMap<>();
        response.put("errors", errors);
        response.put("status", HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        log.error("Unexpected exception: ", ex);
        Map<String, Object> response = new HashMap<>();
        response.put("error", "An unexpected error occurred");
        response.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    private boolean isMultipleBagFetchException(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            String className = current.getClass().getName();
            String message = current.getMessage();
            if (className.contains("MultipleBagFetchException")
                    || (message != null && message.contains("MultipleBagFetchException"))) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
