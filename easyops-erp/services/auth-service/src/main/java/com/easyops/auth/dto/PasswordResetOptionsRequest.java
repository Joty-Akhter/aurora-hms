package com.easyops.auth.dto;

import jakarta.validation.constraints.NotBlank;

public class PasswordResetOptionsRequest {

    @NotBlank(message = "Username or email is required")
    private String usernameOrEmail;

    public PasswordResetOptionsRequest() {
    }

    public String getUsernameOrEmail() {
        return usernameOrEmail;
    }

    public void setUsernameOrEmail(String usernameOrEmail) {
        this.usernameOrEmail = usernameOrEmail;
    }
}
