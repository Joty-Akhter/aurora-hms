package com.easyops.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Refresh Token Request DTO
 * 
 * Data Transfer Object for token refresh requests.
 * 
 * @author EasyOps Team
 * @version 1.0.0
 */
public class RefreshTokenRequest {

    @NotBlank(message = "Refresh token is required")
    private String refreshToken;

    public RefreshTokenRequest() {
    }

    public RefreshTokenRequest(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}

