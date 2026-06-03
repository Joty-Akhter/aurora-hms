package com.easyops.auth.dto;

import jakarta.validation.constraints.NotBlank;

public class ApiKeyValidateRequest {

    @NotBlank
    private String apiKey;

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
}
