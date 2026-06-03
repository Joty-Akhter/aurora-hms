package com.easyops.rbac.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Permission Request DTO
 * 
 * Data Transfer Object for permission creation and update.
 * 
 * @author EasyOps Team
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PermissionRequest {

    @NotBlank(message = "Permission name is required")
    @Size(max = 100, message = "Permission name must not exceed 100 characters")
    private String name;

    @NotBlank(message = "Permission code is required")
    @Size(max = 100, message = "Permission code must not exceed 100 characters")
    private String code;

    @NotBlank(message = "Resource is required")
    @Size(max = 100, message = "Resource must not exceed 100 characters")
    private String resource;

    @NotBlank(message = "Action is required")
    @Size(max = 50, message = "Action must not exceed 50 characters")
    private String action;

    private String description;

    private Boolean isActive = true;
}

