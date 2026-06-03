package com.easyops.organization.dto;

import com.easyops.organization.entity.Location;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Location Response DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationResponse {

    private UUID id;
    private UUID organizationId;
    private String code;
    private String name;
    private String type;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String postalCode;
    private String country;
    private String phone;
    private String email;
    private UUID managerId;
    private String timezone;
    private Map<String, Object> operatingHours;
    private Map<String, Object> coordinates;
    private String status;
    private ZonedDateTime createdAt;
    private ZonedDateTime updatedAt;

    public static LocationResponse fromEntity(Location location) {
        return LocationResponse.builder()
                .id(location.getId())
                .organizationId(location.getOrganizationId())
                .code(location.getCode())
                .name(location.getName())
                .type(location.getType())
                .addressLine1(location.getAddressLine1())
                .addressLine2(location.getAddressLine2())
                .city(location.getCity())
                .state(location.getState())
                .postalCode(location.getPostalCode())
                .country(location.getCountry())
                .phone(location.getPhone())
                .email(location.getEmail())
                .managerId(location.getManagerId())
                .timezone(location.getTimezone())
                .operatingHours(location.getOperatingHours())
                .coordinates(location.getCoordinates())
                .status(location.getStatus())
                .createdAt(location.getCreatedAt())
                .updatedAt(location.getUpdatedAt())
                .build();
    }
}

