package com.easyops.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Password Reset Confirm Request DTO
 * 
 * Data Transfer Object for confirming password reset with new password.
 * 
 * @author EasyOps Team
 * @version 1.0.0
 */
public class PasswordResetConfirmRequest {

    @NotBlank(message = "Verification code is required")
    private String token;

    @NotBlank(message = "New password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String newPassword;

    public PasswordResetConfirmRequest() {
    }

    public PasswordResetConfirmRequest(String token, String newPassword) {
        this.token = token;
        this.newPassword = newPassword;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}

