package com.easyops.organization.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

/**
 * Location Request DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationRequest {

    @NotBlank(message = "Location code is required")
    @Size(max = 50)
    private String code;

    @NotBlank(message = "Location name is required")
    @Size(max = 255)
    private String name;

    private String type;

    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String postalCode;
    private String country;

    private String phone;

    @Email
    private String email;

    private UUID managerId;

    private String timezone;

    private Map<String, Object> operatingHours;

    private Map<String, Object> coordinates;

    private String status;
}

