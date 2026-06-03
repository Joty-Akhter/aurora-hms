package com.easyops.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Login Request DTO
 * 
 * Data Transfer Object for user login requests.
 * 
 * @author EasyOps Team
 * @version 1.0.0
 */
public class LoginRequest {

    @NotBlank(message = "Username or email is required")
    private String usernameOrEmail;

    @NotBlank(message = "Password is required")
    private String password;

    private String ipAddress;
    private String userAgent;

    public LoginRequest() {
    }

    public LoginRequest(String usernameOrEmail, String password, String ipAddress, String userAgent) {
        this.usernameOrEmail = usernameOrEmail;
        this.password = password;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
    }

    public String getUsernameOrEmail() {
        return usernameOrEmail;
    }

    public void setUsernameOrEmail(String usernameOrEmail) {
        this.usernameOrEmail = usernameOrEmail;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }
}

