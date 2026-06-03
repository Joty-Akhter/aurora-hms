package com.easyops.auth.validation;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Aligns password-change / reset rules with {@code password.policy.*} in application.yml.
 */
@Component
public class PasswordPolicyValidator {

    @Value("${password.policy.min-length:8}")
    private int minLength;

    @Value("${password.policy.require-uppercase:true}")
    private boolean requireUppercase;

    @Value("${password.policy.require-lowercase:true}")
    private boolean requireLowercase;

    @Value("${password.policy.require-digit:true}")
    private boolean requireDigit;

    @Value("${password.policy.require-special:true}")
    private boolean requireSpecial;

    public void assertAcceptable(String password) {
        if (password == null || password.isBlank()) {
            throw new RuntimeException("Password is required");
        }
        if (password.length() < minLength) {
            throw new RuntimeException("Password must be at least " + minLength + " characters");
        }
        if (requireUppercase && password.chars().noneMatch(Character::isUpperCase)) {
            throw new RuntimeException("Password must contain at least one uppercase letter");
        }
        if (requireLowercase && password.chars().noneMatch(Character::isLowerCase)) {
            throw new RuntimeException("Password must contain at least one lowercase letter");
        }
        if (requireDigit && password.chars().noneMatch(Character::isDigit)) {
            throw new RuntimeException("Password must contain at least one digit");
        }
        if (requireSpecial && !password.matches(".*[^A-Za-z0-9].*")) {
            throw new RuntimeException("Password must contain at least one special character");
        }
    }
}
