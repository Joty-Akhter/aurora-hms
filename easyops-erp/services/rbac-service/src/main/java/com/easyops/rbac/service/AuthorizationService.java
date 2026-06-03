package com.easyops.rbac.service;

import com.easyops.rbac.dto.AuthorizationRequest;
import com.easyops.rbac.dto.PermissionResponse;
import com.easyops.rbac.dto.RoleResponse;
import com.easyops.rbac.dto.UserOrganizationContextResponse;
import com.easyops.rbac.dto.UserRoleRequest;
import com.easyops.rbac.entity.Permission;
import com.easyops.rbac.entity.Role;
import com.easyops.rbac.entity.UserRole;
import com.easyops.rbac.repository.RoleRepository;
import com.easyops.rbac.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Authorization Service
 * 
 * Service class for authorization and user role management operations.
 * 
 * @author EasyOps Team
 * @version 1.0.0
 */
@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class AuthorizationService {

    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final RoleService roleService;

    /**
     * Assign roles to user
     *
     * Note on caching:
     * userRoles cache keys include both userId and organizationId (or 'all'),
     * so we need to clear all entries for this cache when roles change to avoid stale data.
     */
    @Caching(evict = {
            @CacheEvict(value = "userRoles", allEntries = true),
            @CacheEvict(value = "userPermissions", allEntries = true)
    })
    public List<RoleResponse> assignRolesToUser(UserRoleRequest request) {
        // Delete existing roles for this user and organization
        // If organizationId is null, delete all global roles (organizationId = null)
        // If organizationId is provided, delete only roles for that organization
        if (request.getOrganizationId() == null) {
            // Delete all roles for this user (global roles)
            userRoleRepository.deleteByUserId(request.getUserId());
        } else {
            // Delete only roles for this user and specific organization
            userRoleRepository.deleteByUserIdAndOrganizationId(request.getUserId(), request.getOrganizationId());
        }

        List<Role> roles = roleRepository.findAllById(request.getRoleIds());
        
        if (roles.size() != request.getRoleIds().size()) {
            throw new RuntimeException("Some role IDs are invalid");
        }

        List<UserRole> userRoles = new ArrayList<>();
        for (Role role : roles) {
            UserRole userRole = new UserRole();
            userRole.setUserId(request.getUserId());
            userRole.setRole(role);
            userRole.setOrganizationId(request.getOrganizationId());
            userRole.setExpiresAt(request.getExpiresAt());
            userRoles.add(userRole);
        }

        userRoleRepository.saveAll(userRoles);
        log.info("Assigned {} roles to user {} for organization {}", 
                userRoles.size(), request.getUserId(), request.getOrganizationId());

        return getUserRoles(request.getUserId(), request.getOrganizationId());
    }

    /**
     * Remove role from user
     */
    @Caching(evict = {
            @CacheEvict(value = "userRoles", allEntries = true),
            @CacheEvict(value = "userPermissions", allEntries = true)
    })
    public void removeRoleFromUser(UUID userId, UUID roleId) {
        userRoleRepository.deleteByUserIdAndRoleId(userId, roleId);
        log.info("Removed role {} from user {}", roleId, userId);
    }

    /**
     * Remove all roles from user
     */
    @Caching(evict = {
            @CacheEvict(value = "userRoles", allEntries = true),
            @CacheEvict(value = "userPermissions", allEntries = true)
    })
    public void removeAllRolesFromUser(UUID userId) {
        userRoleRepository.deleteByUserId(userId);
        log.info("Removed all roles from user {}", userId);
    }

    /**
     * Get user roles
     * If organizationId is provided, returns roles for that organization plus global roles (organizationId = null)
     * If organizationId is null, returns all roles (for backward compatibility)
     */
    @Cacheable(value = "userRoles", key = "#userId + '_' + (#organizationId != null ? #organizationId.toString() : 'all')")
    @Transactional(readOnly = true)
    public List<RoleResponse> getUserRoles(UUID userId, UUID organizationId) {
        List<UserRole> userRoles;
        if (organizationId != null) {
            userRoles = userRoleRepository.findActiveRolesByUserIdAndOrganization(
                    userId, organizationId, LocalDateTime.now());
        } else {
            userRoles = userRoleRepository.findActiveRolesByUserId(
                    userId, LocalDateTime.now());
        }
        
        return userRoles.stream()
                .map(ur -> roleService.getRoleById(ur.getRole().getId()))
                .collect(Collectors.toList());
    }

    /**
     * Distinct non-null organization IDs from active role assignments, and whether any assignment is global
     * ({@code organization_id} is null). Used at login to choose tenant context.
     */
    @Transactional(readOnly = true)
    public UserOrganizationContextResponse getUserOrganizationContext(UUID userId) {
        List<UserRole> active = userRoleRepository.findActiveRolesByUserId(userId, LocalDateTime.now());
        Set<UUID> orgIds = active.stream()
                .map(UserRole::getOrganizationId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        boolean hasGlobal = active.stream().anyMatch(ur -> ur.getOrganizationId() == null);
        return new UserOrganizationContextResponse(new ArrayList<>(orgIds), hasGlobal);
    }

    /**
     * Get user permissions
     * If organizationId is provided, returns permissions from roles for that organization plus global roles
     * If organizationId is null, returns all permissions (for backward compatibility)
     */
    @Cacheable(value = "userPermissions", key = "#userId + '_' + (#organizationId != null ? #organizationId.toString() : 'all')")
    @Transactional(readOnly = true)
    public List<PermissionResponse> getUserPermissions(UUID userId, UUID organizationId) {
        List<UserRole> userRoles;
        if (organizationId != null) {
            userRoles = userRoleRepository.findActiveRolesByUserIdAndOrganization(
                    userId, organizationId, LocalDateTime.now());
        } else {
            userRoles = userRoleRepository.findActiveRolesByUserId(
                    userId, LocalDateTime.now());
        }
        
        Set<PermissionResponse> permissions = new HashSet<>();
        for (UserRole userRole : userRoles) {
            // Use Role entity directly from UserRole (EAGER loaded) to avoid Redis cache
            // deserialization issues with cached RoleResponse that may return LinkedHashMap
            Role role = userRole.getRole();
            if (role != null && role.getPermissions() != null) {
                // Access permissions (lazy-loaded, but in transaction so it will initialize)
                role.getPermissions().stream()
                        .map(this::mapToPermissionResponse)
                        .forEach(permissions::add);
            }
        }
        
        return new ArrayList<>(permissions);
    }

    /**
     * Check if user has permission
     */
    @Transactional(readOnly = true)
    public boolean hasPermission(AuthorizationRequest request) {
        List<PermissionResponse> permissions = getUserPermissions(
                request.getUserId(), request.getOrganizationId());
        
        return permissions.stream()
                .anyMatch(p -> p.getResource().equals(request.getResource()) 
                        && p.getAction().equals(request.getAction())
                        && p.getIsActive());
    }

    /**
     * Check if user has role
     */
    @Transactional(readOnly = true)
    public boolean hasRole(UUID userId, String roleCode, UUID organizationId) {
        List<RoleResponse> roles = getUserRoles(userId, organizationId);
        
        return roles.stream()
                .anyMatch(r -> r.getCode().equals(roleCode) && r.getIsActive());
    }

    /**
     * Get users by role
     */
    @Transactional(readOnly = true)
    public List<UUID> getUsersByRole(UUID roleId) {
        return userRoleRepository.findByRoleId(roleId).stream()
                .map(UserRole::getUserId)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * Clean up expired roles
     */
    @Caching(evict = {
            @CacheEvict(value = "userRoles", allEntries = true),
            @CacheEvict(value = "userPermissions", allEntries = true)
    })
    public void cleanupExpiredRoles() {
        userRoleRepository.deleteExpiredRoles(LocalDateTime.now());
        log.info("Cleaned up expired user roles");
    }

    /**
     * Map Permission entity to PermissionResponse DTO
     */
    private PermissionResponse mapToPermissionResponse(Permission permission) {
        PermissionResponse response = new PermissionResponse();
        response.setId(permission.getId());
        response.setName(permission.getName());
        response.setCode(permission.getCode());
        response.setResource(permission.getResource());
        response.setAction(permission.getAction());
        response.setDescription(permission.getDescription());
        response.setIsActive(permission.getIsActive());
        response.setCreatedAt(permission.getCreatedAt());
        response.setUpdatedAt(permission.getUpdatedAt());
        return response;
    }
}

