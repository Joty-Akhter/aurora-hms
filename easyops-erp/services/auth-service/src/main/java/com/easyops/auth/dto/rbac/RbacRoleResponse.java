package com.easyops.auth.dto.rbac;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Representation of a role as returned by the RBAC service.
 */
public class RbacRoleResponse {

    private UUID id;
    private String name;
    private String code;
    private String description;
    private Boolean isSystemRole;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Set<RbacPermissionResponse> permissions = new HashSet<>();

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getIsSystemRole() {
        return isSystemRole;
    }

    public void setIsSystemRole(Boolean systemRole) {
        isSystemRole = systemRole;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean active) {
        isActive = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Set<RbacPermissionResponse> getPermissions() {
        return permissions;
    }

    public void setPermissions(Set<RbacPermissionResponse> permissions) {
        this.permissions = permissions;
    }
}

