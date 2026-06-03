package com.easyops.rbac.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Permission Response DTO
 *
 * Data Transfer Object for permission responses.
 *
 * @author EasyOps Team
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PermissionResponse {

    private UUID id;
    private String name;
    private String code;
    private String resource;
    private String action;
    private String description;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

