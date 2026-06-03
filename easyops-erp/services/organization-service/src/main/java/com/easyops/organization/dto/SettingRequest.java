package com.easyops.organization.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Setting Request DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettingRequest {

    @NotBlank(message = "Setting key is required")
    private String key;

    private String value;

    private String type;

    private Boolean encrypted;
}

