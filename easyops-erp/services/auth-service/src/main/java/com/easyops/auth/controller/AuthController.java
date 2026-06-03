package com.easyops.auth.controller;

import com.easyops.auth.dto.*;
import com.easyops.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Authentication Controller
 * 
 * REST controller for authentication and authorization operations.
 * 
 * @author EasyOps Team
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Test endpoint to generate password hash
     */
    @GetMapping("/generate-hash/{password}")
    public ResponseEntity<Map<String, String>> generateHash(@PathVariable("password") String password) {
        org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder encoder = 
            new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
        String hash = encoder.encode(password);
        Map<String, String> response = new HashMap<>();
        response.put("password", password);
        response.put("hash", hash);
        return ResponseEntity.ok(response);
    }

    /**
     * Login endpoint
     * 
     * @param request Login request with credentials
     * @return Login response with JWT tokens
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            @RequestHeader(value = "X-Forwarded-For", required = false) String ipAddress,
            @RequestHeader(value = "User-Agent", required = false) String userAgent,
            @RequestHeader(value = "Origin", required = false) String origin,
            @RequestHeader(value = "Host", required = false) String host) {
        
        log.debug("[Auth] Login request received: user={} origin={} host={} x-forwarded-for={}",
                request.getUsernameOrEmail(), origin, host, ipAddress);

        request.setIpAddress(ipAddress);
        request.setUserAgent(userAgent);

        try {
            LoginResponse response = authService.login(request);
            log.debug("[Auth] Login success: user={}", request.getUsernameOrEmail());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.debug("[Auth] Login failed: user={} reason={}", request.getUsernameOrEmail(), e.getMessage());
            throw e;
        }
    }

    /**
     * Logout endpoint
     * 
     * @param authHeader Authorization header with JWT token
     * @return Success message
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            authService.logout(token);
        }
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Logged out successfully");
        return ResponseEntity.ok(response);
    }

    /**
     * Refresh token endpoint
     * 
     * @param request Refresh token request
     * @return New JWT tokens
     */
    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        LoginResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Forgot-password: which OTP channels exist for this identifier (masked).
     */
    @PostMapping("/password/reset/options")
    public ResponseEntity<PasswordResetOptionsResponse> passwordResetOptions(
            @Valid @RequestBody PasswordResetOptionsRequest request) {
        String identifier = request.getUsernameOrEmail() != null ? request.getUsernameOrEmail().trim() : "";
        PasswordResetOptionsResponse body = authService.getPasswordResetOptions(identifier);
        return ResponseEntity.ok(body);
    }

    /**
     * Initiate password reset — sends OTP by EMAIL or SMS via communication-service.
     */
    @PostMapping("/password/reset")
    public ResponseEntity<Map<String, String>> initiatePasswordReset(
            @Valid @RequestBody PasswordResetRequest request) {

        authService.initiatePasswordReset(request);

        Map<String, String> response = new HashMap<>();
        response.put("message", "If the account exists, a verification code has been sent.");
        return ResponseEntity.ok(response);
    }

    /**
     * Confirm password reset
     * 
     * @param request Password reset confirmation with token and new password
     * @return Success message
     */
    @PostMapping("/password/reset/confirm")
    public ResponseEntity<Map<String, String>> confirmPasswordReset(
            @Valid @RequestBody PasswordResetConfirmRequest request) {
        
        authService.confirmPasswordReset(request);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Password has been reset successfully");
        return ResponseEntity.ok(response);
    }

    /**
     * Change password for authenticated user
     * 
     * @param request Change password request
     * @param userId User ID from path
     * @return Success message
     */
    @PostMapping("/password/change/{userId}")
    public ResponseEntity<Map<String, String>> changePassword(
            @PathVariable("userId") UUID userId,
            @Valid @RequestBody ChangePasswordRequest request) {
        
        authService.changePassword(userId, request);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Password changed successfully");
        return ResponseEntity.ok(response);
    }

    /**
     * Validate token
     * 
     * @param authHeader Authorization header with JWT token
     * @return Validation result
     */
    @GetMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateToken(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        Map<String, Object> response = new HashMap<>();
        
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            boolean isValid = authService.validateToken(token);
            
            response.put("valid", isValid);
            return ResponseEntity.ok(response);
        }
        
        response.put("valid", false);
        return ResponseEntity.ok(response);
    }

    /**
     * Health check endpoint
     * 
     * @return Health status
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "Authentication Service");
        return ResponseEntity.ok(response);
    }

    /**
     * Exception handler for runtime exceptions
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntimeException(RuntimeException ex) {
        log.error("Error: {}", ex.getMessage());
        
        Map<String, String> response = new HashMap<>();
        response.put("error", ex.getMessage());
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
}

