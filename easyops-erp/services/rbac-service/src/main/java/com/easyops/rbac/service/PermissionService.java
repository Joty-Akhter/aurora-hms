package com.easyops.rbac.service;

import com.easyops.rbac.dto.PermissionRequest;
import com.easyops.rbac.dto.PermissionResponse;
import com.easyops.rbac.entity.Permission;
import com.easyops.rbac.repository.PermissionRepository;
import com.easyops.rbac.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Permission Service
 * 
 * Service class for permission management operations.
 * 
 * @author EasyOps Team
 * @version 1.0.0
 */
@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class PermissionService {

    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;

    /**
     * Create a new permission
     */
    @Caching(evict = {
            @CacheEvict(value = "permissions", allEntries = true),
            @CacheEvict(value = "userPermissions", allEntries = true),
            @CacheEvict(value = "activePermissions", allEntries = true)
    })
    public PermissionResponse createPermission(PermissionRequest request) {
        if (permissionRepository.existsByCode(request.getCode())) {
            throw new RuntimeException("Permission with code " + request.getCode() + " already exists");
        }

        Permission permission = new Permission();
        permission.setName(request.getName());
        permission.setCode(request.getCode());
        permission.setResource(request.getResource());
        permission.setAction(request.getAction());
        permission.setDescription(request.getDescription());
        permission.setIsActive(request.getIsActive());

        Permission savedPermission = permissionRepository.save(permission);
        log.info("Created permission: {}", savedPermission.getCode());

        assignPermissionToSystemAdmin(savedPermission);
        
        return mapToPermissionResponse(savedPermission);
    }

    /**
     * Get permission by ID
     */
    @Cacheable(value = "permissions", key = "#id")
    @Transactional(readOnly = true)
    public PermissionResponse getPermissionById(UUID id) {
        Permission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Permission not found with ID: " + id));
        return mapToPermissionResponse(permission);
    }

    /**
     * Get permission by code
     */
    @Cacheable(value = "permissions", key = "#code")
    @Transactional(readOnly = true)
    public PermissionResponse getPermissionByCode(String code) {
        Permission permission = permissionRepository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("Permission not found with code: " + code));
        return mapToPermissionResponse(permission);
    }

    /**
     * Get all permissions
     */
    @Transactional(readOnly = true)
    public Page<PermissionResponse> getAllPermissions(Pageable pageable) {
        return permissionRepository.findAll(pageable)
                .map(this::mapToPermissionResponse);
    }

    /**
     * Get all active permissions
     */
    @Cacheable(value = "activePermissions")
    @Transactional(readOnly = true)
    public List<PermissionResponse> getActivePermissions() {
        return permissionRepository.findByIsActiveTrue().stream()
                .map(this::mapToPermissionResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get permissions by resource
     */
    @Transactional(readOnly = true)
    public List<PermissionResponse> getPermissionsByResource(String resource) {
        return permissionRepository.findByResource(resource).stream()
                .map(this::mapToPermissionResponse)
                .collect(Collectors.toList());
    }

    /**
     * Update permission
     */
    @Caching(evict = {
            @CacheEvict(value = "permissions", allEntries = true),
            @CacheEvict(value = "userPermissions", allEntries = true),
            @CacheEvict(value = "activePermissions", allEntries = true)
    })
    public PermissionResponse updatePermission(UUID id, PermissionRequest request) {
        Permission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Permission not found with ID: " + id));

        permission.setName(request.getName());
        permission.setResource(request.getResource());
        permission.setAction(request.getAction());
        permission.setDescription(request.getDescription());
        permission.setIsActive(request.getIsActive());

        Permission savedPermission = permissionRepository.save(permission);
        log.info("Updated permission: {}", savedPermission.getCode());
        
        return mapToPermissionResponse(savedPermission);
    }

    /**
     * Delete permission
     */
    @Caching(evict = {
            @CacheEvict(value = "permissions", allEntries = true),
            @CacheEvict(value = "userPermissions", allEntries = true),
            @CacheEvict(value = "activePermissions", allEntries = true)
    })
    public void deletePermission(UUID id) {
        Permission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Permission not found with ID: " + id));

        permissionRepository.delete(permission);
        log.info("Deleted permission: {}", permission.getCode());
    }

    /**
     * Search permissions
     */
    @Transactional(readOnly = true)
    public List<PermissionResponse> searchPermissions(String searchTerm) {
        return permissionRepository.searchPermissions(searchTerm).stream()
                .map(this::mapToPermissionResponse)
                .collect(Collectors.toList());
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

    /**
     * Ensure the SYSTEM_ADMIN role always has every permission.
     */
    private void assignPermissionToSystemAdmin(Permission permission) {
        roleRepository.assignPermissionToRole("SYSTEM_ADMIN", permission.getId());
        log.info("Assigned permission {} to SYSTEM_ADMIN role", permission.getCode());
    }
}

