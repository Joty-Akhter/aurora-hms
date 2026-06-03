package com.easyops.rbac.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Authorization Request DTO
 * 
 * Data Transfer Object for checking user permissions.
 * 
 * @author EasyOps Team
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthorizationRequest {

    @NotNull(message = "User ID is required")
    private UUID userId;

    @NotBlank(message = "Resource is required")
    private String resource;

    @NotBlank(message = "Action is required")
    private String action;

    private UUID organizationId;
}

