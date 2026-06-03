package com.easyops.rbac.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;
import java.util.UUID;

/**
 * Role Request DTO
 * 
 * Data Transfer Object for role creation and update.
 * 
 * @author EasyOps Team
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoleRequest {

    @NotBlank(message = "Role name is required")
    @Size(max = 100, message = "Role name must not exceed 100 characters")
    private String name;

    @NotBlank(message = "Role code is required")
    @Size(max = 50, message = "Role code must not exceed 50 characters")
    private String code;

    private String description;

    private Boolean isActive = true;

    private Set<UUID> permissionIds;
}

