package com.easyops.hr.config;

import com.easyops.hr.exception.ResourceConflictException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class HrExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(HrExceptionHandler.class);

    @ExceptionHandler(ResourceConflictException.class)
    public ResponseEntity<Map<String, String>> handleConflict(ResourceConflictException ex) {
        log.warn("Conflict: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("message", ex.getMessage() != null ? ex.getMessage() : "Resource conflict"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Bad request: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", ex.getMessage() != null ? ex.getMessage() : "Invalid request"));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleDataIntegrity(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation: {}", ex.getMessage());
        String message = "Duplicate or invalid reference. Check unique constraints (e.g. email, employee number) and foreign keys (department, position, manager).";
        if (ex.getCause() != null && ex.getCause().getMessage() != null) {
            String detail = ex.getCause().getMessage();
            if (detail.contains("unique") || detail.contains("duplicate")) {
                message = "Duplicate value. This email, employee number, or other unique field may already exist.";
            } else if (detail.contains("foreign key") || detail.contains("violates foreign key")) {
                message = "Invalid reference. The department, position, or manager does not exist.";
            }
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", message));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntime(RuntimeException ex) {
        String msg = ex.getMessage() != null ? ex.getMessage() : "Internal error";
        if (msg.contains("not found") || msg.contains("NotFound")) {
            log.warn("Not found: {}", msg);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", msg));
        }
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "An error occurred. Please try again."));
    }
}
