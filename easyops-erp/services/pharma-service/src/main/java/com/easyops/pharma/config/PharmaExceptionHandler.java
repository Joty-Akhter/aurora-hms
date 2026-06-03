package com.easyops.pharma.config;

import com.easyops.pharma.exception.IncentiveRuleValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class PharmaExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(PharmaExceptionHandler.class);

    @ExceptionHandler(IncentiveRuleValidationException.class)
    public ResponseEntity<Map<String, String>> handleIncentiveRuleValidation(IncentiveRuleValidationException ex) {
        log.warn("Incentive rule validation failed: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", ex.getMessage()));
    }
}
