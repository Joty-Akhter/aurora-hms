package com.easyops.organization.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Organization Request DTO
 * Used for creating and updating organizations
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationRequest {

    @NotBlank(message = "Organization code is required")
    @Size(max = 20, message = "Code must be at most 20 characters")
    @Pattern(regexp = "^[A-Z0-9_-]+$", message = "Code must contain only uppercase letters, numbers, underscores and hyphens")
    private String code;

    @NotBlank(message = "Organization name is required")
    @Size(max = 255, message = "Name must be at most 255 characters")
    private String name;

    @Size(max = 255, message = "Legal name must be at most 255 characters")
    private String legalName;

    private String description;

    private String logo;

    @Email(message = "Invalid email format")
    @Size(max = 255)
    private String email;

    @Pattern(regexp = "^[+]?[0-9\\s\\-()]+$", message = "Invalid phone number format")
    @Size(max = 50)
    private String phone;

    @Size(max = 255)
    private String website;

    // Business Information
    private String industry;
    private String businessType;
    private String taxId;
    private String registrationNumber;
    private LocalDate fiscalYearStart;

    @Size(min = 3, max = 3, message = "Currency code must be exactly 3 characters")
    private String currency;

    private String timezone;
    private String locale;

    // Address Information
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String postalCode;

    @Size(min = 2, max = 2, message = "Country code must be exactly 2 characters")
    private String country;

    // Theme / Branding
    @Pattern(regexp = "^(light|dark)$", message = "Theme mode must be 'light' or 'dark'")
    private String themeMode;

    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Color must be a valid hex color (e.g. #2563eb)")
    private String themePrimaryColor;

    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Color must be a valid hex color")
    private String themeSecondaryColor;

    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Color must be a valid hex color")
    private String themeAccentColor;

    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Color must be a valid hex color")
    private String themeSidebarColor;

    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Color must be a valid hex color")
    private String themeSidebarTextColor;

    // Subscription Information
    private String subscriptionPlan;
    private Integer maxUsers;
    private Long maxStorage;
}

