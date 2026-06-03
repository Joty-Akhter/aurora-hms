package com.easyops.auth.dto;

import jakarta.validation.constraints.Email;

/**
 * Starts OTP delivery for password reset.
 * Legacy: {@code email} only implies usernameOrEmail = email and channel = EMAIL.
 */
public class PasswordResetRequest {

    @Email(message = "Email should be valid")
    private String email;

    private String usernameOrEmail;

    /** EMAIL or SMS */
    private String channel;

    public PasswordResetRequest() {
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUsernameOrEmail() {
        return usernameOrEmail;
    }

    public void setUsernameOrEmail(String usernameOrEmail) {
        this.usernameOrEmail = usernameOrEmail;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }
}
