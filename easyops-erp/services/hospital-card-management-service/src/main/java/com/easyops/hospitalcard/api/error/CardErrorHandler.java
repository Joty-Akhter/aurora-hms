package com.easyops.hospitalcard.api.error;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class CardErrorHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(
            IllegalArgumentException ex, HttpServletRequest request) {
        String msg = ex.getMessage() != null ? ex.getMessage() : "";
        HttpStatus status;
        String code;
        if (msg.startsWith("UNAUTHORIZED:")) {
            status = HttpStatus.UNAUTHORIZED;
            code = "UNAUTHORIZED";
        } else if (msg.startsWith("ACCESS_DENIED:")) {
            status = HttpStatus.FORBIDDEN;
            code = "FORBIDDEN";
        } else if (msg.startsWith("IDEMPOTENCY_CONFLICT:")) {
            status = HttpStatus.CONFLICT;
            code = "IDEMPOTENCY_CONFLICT";
        } else if (msg.contains("not found")) {
            status = HttpStatus.NOT_FOUND;
            code = "NOT_FOUND";
        } else if (msg.contains("already exists") || msg.contains("duplicate")) {
            status = HttpStatus.CONFLICT;
            code = "CONFLICT";
        } else {
            status = HttpStatus.BAD_REQUEST;
            code = "BAD_REQUEST";
        }
        return buildErrorResponse(status, code, msg, request.getRequestURI());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        StringBuilder sb = new StringBuilder("Validation failed");
        if (!ex.getBindingResult().getAllErrors().isEmpty()) {
            sb.append(": ");
            ex.getBindingResult().getAllErrors().forEach(error -> {
                if (error instanceof FieldError fieldError) {
                    sb.append(fieldError.getField()).append(" ").append(fieldError.getDefaultMessage()).append("; ");
                } else {
                    sb.append(error.getDefaultMessage()).append("; ");
                }
            });
        }
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "BAD_REQUEST", sb.toString(), request.getRequestURI());
    }

    @ExceptionHandler({DataIntegrityViolationException.class, IllegalStateException.class})
    public ResponseEntity<Map<String, Object>> handleConflict(Exception ex, HttpServletRequest request) {
        String message = ex.getMessage() != null ? ex.getMessage() : "Conflict";
        return buildErrorResponse(HttpStatus.CONFLICT, "CONFLICT", message, request.getRequestURI());
    }

    private ResponseEntity<Map<String, Object>> buildErrorResponse(
            HttpStatus status,
            String code,
            String message,
            String path) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", OffsetDateTime.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("code", code);
        body.put("message", message);
        body.put("path", path);
        return ResponseEntity.status(status).body(body);
    }
}
