package com.easyops.organization.service;

import com.easyops.organization.dto.LocationRequest;
import com.easyops.organization.dto.LocationResponse;
import com.easyops.organization.entity.Location;
import com.easyops.organization.repository.LocationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Location Service
 * Handles business logic for location management
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LocationService {

    private final LocationRepository locationRepository;

    /**
     * Create a new location
     */
    @Transactional
    public LocationResponse createLocation(UUID organizationId, LocationRequest request) {
        log.info("Creating location for organization: {}", organizationId);

        // Check if code already exists
        if (locationRepository.existsByOrganizationIdAndCode(organizationId, request.getCode())) {
            throw new IllegalArgumentException("Location code already exists: " + request.getCode());
        }

        Location location = Location.builder()
                .organizationId(organizationId)
                .code(request.getCode())
                .name(request.getName())
                .type(request.getType() != null ? request.getType() : "BRANCH")
                .addressLine1(request.getAddressLine1())
                .addressLine2(request.getAddressLine2())
                .city(request.getCity())
                .state(request.getState())
                .postalCode(request.getPostalCode())
                .country(request.getCountry())
                .phone(request.getPhone())
                .email(request.getEmail())
                .managerId(request.getManagerId())
                .timezone(request.getTimezone())
                .operatingHours(request.getOperatingHours())
                .coordinates(request.getCoordinates())
                .status(request.getStatus() != null ? request.getStatus() : "ACTIVE")
                .build();

        Location saved = locationRepository.save(location);
        log.info("Location created successfully: {}", saved.getId());

        return LocationResponse.fromEntity(saved);
    }

    /**
     * Get location by ID
     */
    public LocationResponse getLocationById(UUID id) {
        log.debug("Fetching location by ID: {}", id);
        Location location = locationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Location not found: " + id));
        return LocationResponse.fromEntity(location);
    }

    /**
     * Get all locations for organization
     */
    public List<LocationResponse> getLocationsByOrganization(UUID organizationId) {
        log.debug("Fetching locations for organization: {}", organizationId);
        return locationRepository.findByOrganizationId(organizationId).stream()
                .map(LocationResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get locations by type
     */
    public List<LocationResponse> getLocationsByType(UUID organizationId, String type) {
        log.debug("Fetching locations by type: {} for organization: {}", type, organizationId);
        return locationRepository.findByOrganizationIdAndType(organizationId, type).stream()
                .map(LocationResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Update location
     */
    @Transactional
    public LocationResponse updateLocation(UUID id, LocationRequest request) {
        log.info("Updating location: {}", id);

        Location location = locationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Location not found: " + id));

        if (request.getName() != null) location.setName(request.getName());
        if (request.getType() != null) location.setType(request.getType());
        if (request.getAddressLine1() != null) location.setAddressLine1(request.getAddressLine1());
        if (request.getAddressLine2() != null) location.setAddressLine2(request.getAddressLine2());
        if (request.getCity() != null) location.setCity(request.getCity());
        if (request.getState() != null) location.setState(request.getState());
        if (request.getPostalCode() != null) location.setPostalCode(request.getPostalCode());
        if (request.getCountry() != null) location.setCountry(request.getCountry());
        if (request.getPhone() != null) location.setPhone(request.getPhone());
        if (request.getEmail() != null) location.setEmail(request.getEmail());
        if (request.getManagerId() != null) location.setManagerId(request.getManagerId());
        if (request.getTimezone() != null) location.setTimezone(request.getTimezone());
        if (request.getOperatingHours() != null) location.setOperatingHours(request.getOperatingHours());
        if (request.getCoordinates() != null) location.setCoordinates(request.getCoordinates());
        if (request.getStatus() != null) location.setStatus(request.getStatus());

        Location updated = locationRepository.save(location);
        log.info("Location updated successfully: {}", id);

        return LocationResponse.fromEntity(updated);
    }

    /**
     * Delete location
     */
    @Transactional
    public void deleteLocation(UUID id) {
        log.info("Deleting location: {}", id);

        Location location = locationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Location not found: " + id));

        locationRepository.delete(location);
        log.info("Location deleted successfully: {}", id);
    }
}

