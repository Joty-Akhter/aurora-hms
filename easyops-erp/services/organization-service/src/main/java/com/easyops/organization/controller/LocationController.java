package com.easyops.organization.controller;

import com.easyops.organization.dto.LocationRequest;
import com.easyops.organization.dto.LocationResponse;
import com.easyops.organization.security.OrganizationRbacService;
import com.easyops.organization.security.RbacRequestHeaders;
import com.easyops.organization.service.LocationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Location Controller
 */
@RestController
@RequestMapping("/api/organizations/{organizationId}/locations")
@RequiredArgsConstructor
@Slf4j
public class LocationController {

    private final LocationService locationService;
    private final OrganizationRbacService organizationRbac;

    @PostMapping
    public ResponseEntity<LocationResponse> createLocation(
            @PathVariable UUID organizationId,
            @Valid @RequestBody LocationRequest request,
            @RequestHeader("X-User-Id") String userId) {
        UUID actor = RbacRequestHeaders.requireUserId(userId);
        organizationRbac.requireOrgWrite(actor, organizationId);
        LocationResponse response = locationService.createLocation(organizationId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<LocationResponse>> getLocations(
            @PathVariable UUID organizationId,
            @RequestHeader("X-User-Id") String userId) {
        UUID actor = RbacRequestHeaders.requireUserId(userId);
        organizationRbac.requireOrgRead(actor, organizationId);
        List<LocationResponse> response = locationService.getLocationsByOrganization(organizationId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<List<LocationResponse>> getLocationsByType(
            @PathVariable UUID organizationId,
            @PathVariable String type,
            @RequestHeader("X-User-Id") String userId) {
        UUID actor = RbacRequestHeaders.requireUserId(userId);
        organizationRbac.requireOrgRead(actor, organizationId);
        List<LocationResponse> response = locationService.getLocationsByType(organizationId, type);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{locationId}")
    public ResponseEntity<LocationResponse> getLocationById(
            @PathVariable UUID organizationId,
            @PathVariable UUID locationId,
            @RequestHeader("X-User-Id") String userId) {
        UUID actor = RbacRequestHeaders.requireUserId(userId);
        organizationRbac.requireOrgRead(actor, organizationId);
        LocationResponse response = locationService.getLocationById(locationId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{locationId}")
    public ResponseEntity<LocationResponse> updateLocation(
            @PathVariable UUID organizationId,
            @PathVariable UUID locationId,
            @Valid @RequestBody LocationRequest request,
            @RequestHeader("X-User-Id") String userId) {
        UUID actor = RbacRequestHeaders.requireUserId(userId);
        organizationRbac.requireOrgWrite(actor, organizationId);
        LocationResponse response = locationService.updateLocation(locationId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{locationId}")
    public ResponseEntity<Void> deleteLocation(
            @PathVariable UUID organizationId,
            @PathVariable UUID locationId,
            @RequestHeader("X-User-Id") String userId) {
        UUID actor = RbacRequestHeaders.requireUserId(userId);
        organizationRbac.requireOrgWrite(actor, organizationId);
        locationService.deleteLocation(locationId);
        return ResponseEntity.noContent().build();
    }
}
