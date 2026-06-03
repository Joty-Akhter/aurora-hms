package com.easyops.organization.controller;

import com.easyops.organization.dto.OrganizationRequest;
import com.easyops.organization.dto.OrganizationResponse;
import com.easyops.organization.dto.OrganizationThemeRequest;
import com.easyops.organization.security.OrganizationRbacService;
import com.easyops.organization.security.RbacRequestHeaders;
import com.easyops.organization.service.OrganizationLogoService;
import com.easyops.organization.service.OrganizationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Organization Controller
 * REST endpoints for organization management
 */
@RestController
@RequestMapping("/api/organizations")
@RequiredArgsConstructor
@Slf4j
public class OrganizationController {

    private final OrganizationService organizationService;
    private final OrganizationLogoService organizationLogoService;
    private final OrganizationRbacService organizationRbac;

    /**
     * Create new organization
     */
    @PostMapping
    public ResponseEntity<OrganizationResponse> createOrganization(
            @Valid @RequestBody OrganizationRequest request,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userId);
        organizationRbac.requireOrgWrite(actor, organizationId);
        UUID createdBy = actor;
        OrganizationResponse response = organizationService.createOrganization(request, createdBy);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Organizations for the current user (from admin.user_organizations). Uses X-User-Id when JWT context is not wired.
     */
    @GetMapping("/me")
    public ResponseEntity<List<OrganizationResponse>> getMyOrganizations(
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        if (userId == null || userId.isBlank()) {
            return ResponseEntity.ok(Collections.emptyList());
        }
        try {
            return ResponseEntity.ok(organizationService.getOrganizationsForUser(UUID.fromString(userId)));
        } catch (IllegalArgumentException ex) {
            log.warn("Invalid X-User-Id for /me: {}", userId);
            return ResponseEntity.ok(Collections.emptyList());
        }
    }

    /**
     * Upload a logo image (JPEG, PNG, WebP, GIF, SVG; max 2 MB). Sets {@code logo} to the served path.
     */
    @PostMapping(value = "/{id}/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<OrganizationResponse> uploadLogo(
            @PathVariable("id") UUID id,
            @RequestParam("file") MultipartFile file,
            @RequestHeader("X-User-Id") String userId) {
        UUID actor = RbacRequestHeaders.requireUserId(userId);
        organizationRbac.requireOrgWrite(actor, id);
        OrganizationResponse response = organizationService.uploadLogo(id, file, actor);
        return ResponseEntity.ok(response);
    }

    /**
     * Serve uploaded logo bytes (for {@code <img src>} — no auth required at service layer).
     */
    @GetMapping("/{id}/logo")
    public ResponseEntity<Resource> getLogo(@PathVariable("id") UUID id) throws IOException {
        Optional<Resource> resource = organizationLogoService.loadAsResource(id);
        if (resource.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(organizationLogoService.mediaTypeForOrg(id))
                .body(resource.get());
    }

    /**
     * Get organization by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<OrganizationResponse> getOrganizationById(
            @PathVariable("id") UUID id,
            @RequestHeader("X-User-Id") String userId) {
        UUID actor = RbacRequestHeaders.requireUserId(userId);
        organizationRbac.requireOrgRead(actor, id);
        OrganizationResponse response = organizationService.getOrganizationById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Get organization by code
     */
    @GetMapping("/code/{code}")
    public ResponseEntity<OrganizationResponse> getOrganizationByCode(
            @PathVariable("code") String code,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userId);
        organizationRbac.requireOrgRead(actor, organizationId);
        OrganizationResponse response = organizationService.getOrganizationByCode(code);
        return ResponseEntity.ok(response);
    }

    /**
     * Get all organizations (paginated)
     */
    @GetMapping
    public ResponseEntity<Page<OrganizationResponse>> getAllOrganizations(
            Pageable pageable,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userId);
        organizationRbac.requireOrgRead(actor, organizationId);
        Page<OrganizationResponse> response = organizationService.getAllOrganizations(pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Get organizations by status
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<OrganizationResponse>> getOrganizationsByStatus(
            @PathVariable("status") String status,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userId);
        organizationRbac.requireOrgRead(actor, organizationId);
        List<OrganizationResponse> response = organizationService.getOrganizationsByStatus(status);
        return ResponseEntity.ok(response);
    }

    /**
     * Update organization
     */
    @PutMapping("/{id}")
    public ResponseEntity<OrganizationResponse> updateOrganization(
            @PathVariable("id") UUID id,
            @Valid @RequestBody OrganizationRequest request,
            @RequestHeader("X-User-Id") String userId) {
        UUID actor = RbacRequestHeaders.requireUserId(userId);
        organizationRbac.requireOrgWrite(actor, id);
        OrganizationResponse response = organizationService.updateOrganization(id, request, actor);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete organization
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrganization(
            @PathVariable("id") UUID id,
            @RequestHeader("X-User-Id") String userId) {
        UUID actor = RbacRequestHeaders.requireUserId(userId);
        organizationRbac.requireOrgWrite(actor, id);
        organizationService.deleteOrganization(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Activate organization
     */
    @PatchMapping("/{id}/activate")
    public ResponseEntity<OrganizationResponse> activateOrganization(
            @PathVariable("id") UUID id,
            @RequestHeader("X-User-Id") String userId) {
        UUID actor = RbacRequestHeaders.requireUserId(userId);
        organizationRbac.requireOrgWrite(actor, id);
        OrganizationResponse response = organizationService.activateOrganization(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Suspend organization
     */
    @PatchMapping("/{id}/suspend")
    public ResponseEntity<OrganizationResponse> suspendOrganization(
            @PathVariable("id") UUID id,
            @RequestHeader("X-User-Id") String userId) {
        UUID actor = RbacRequestHeaders.requireUserId(userId);
        organizationRbac.requireOrgWrite(actor, id);
        OrganizationResponse response = organizationService.suspendOrganization(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Update theme / branding
     */
    @PatchMapping("/{id}/theme")
    public ResponseEntity<OrganizationResponse> updateTheme(
            @PathVariable("id") UUID id,
            @Valid @RequestBody OrganizationThemeRequest request,
            @RequestHeader("X-User-Id") String userId) {
        UUID actor = RbacRequestHeaders.requireUserId(userId);
        organizationRbac.requireOrgWrite(actor, id);
        OrganizationResponse response = organizationService.updateTheme(id, request, actor);
        return ResponseEntity.ok(response);
    }

    /**
     * Update subscription plan
     */
    @PatchMapping("/{id}/subscription")
    public ResponseEntity<OrganizationResponse> updateSubscription(
            @PathVariable("id") UUID id,
            @RequestParam("plan") String plan,
            @RequestParam(value = "maxUsers", required = false) Integer maxUsers,
            @RequestParam(value = "maxStorage", required = false) Long maxStorage,
            @RequestHeader("X-User-Id") String userId) {
        UUID actor = RbacRequestHeaders.requireUserId(userId);
        organizationRbac.requireOrgWrite(actor, id);
        OrganizationResponse response = organizationService.updateSubscriptionPlan(id, plan, maxUsers, maxStorage);
        return ResponseEntity.ok(response);
    }
}
