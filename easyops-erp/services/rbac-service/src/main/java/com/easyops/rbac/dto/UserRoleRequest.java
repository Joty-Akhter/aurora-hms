package com.easyops.rbac.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * User Role Request DTO
 * 
 * Data Transfer Object for assigning roles to users.
 * 
 * @author EasyOps Team
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRoleRequest {

    @NotNull(message = "User ID is required")
    private UUID userId;

    @NotNull(message = "At least one role ID is required")
    private Set<UUID> roleIds;

    private UUID organizationId;

    private LocalDateTime expiresAt;
}

