package com.easyops.organization.dto;

import com.easyops.organization.entity.Organization;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Organization Response DTO
 * Used for returning organization data
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationResponse {

    private UUID id;
    private String code;
    private String name;
    private String legalName;
    private String description;
    private String logo;
    private String website;
    private String email;
    private String phone;

    // Business Information
    private String industry;
    private String businessType;
    private String taxId;
    private String registrationNumber;
    private LocalDate fiscalYearStart;
    private String currency;
    private String timezone;
    private String locale;

    // Address Information
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String postalCode;
    private String country;

    // Subscription Information
    private String subscriptionPlan;
    private String subscriptionStatus;
    private ZonedDateTime subscriptionStartDate;
    private ZonedDateTime subscriptionEndDate;
    private ZonedDateTime trialEndsAt;
    private Integer maxUsers;
    private Long maxStorage;

    // Theme / Branding
    private String themeMode;
    private String themePrimaryColor;
    private String themeSecondaryColor;
    private String themeAccentColor;
    private String themeSidebarColor;
    private String themeSidebarTextColor;

    // Status & Metadata
    private String status;
    private Boolean isActive;
    private ZonedDateTime createdAt;
    private ZonedDateTime updatedAt;
    private UUID createdBy;
    private UUID updatedBy;

    /**
     * Convert Organization entity to response DTO
     */
    public static OrganizationResponse fromEntity(Organization org) {
        return OrganizationResponse.builder()
                .id(org.getId())
                .code(org.getCode())
                .name(org.getName())
                .legalName(org.getLegalName())
                .description(org.getDescription())
                .logo(org.getLogo())
                .website(org.getWebsite())
                .email(org.getEmail())
                .phone(org.getPhone())
                .industry(org.getIndustry())
                .businessType(org.getBusinessType())
                .taxId(org.getTaxId())
                .registrationNumber(org.getRegistrationNumber())
                .fiscalYearStart(org.getFiscalYearStart())
                .currency(org.getCurrency())
                .timezone(org.getTimezone())
                .locale(org.getLocale())
                .addressLine1(org.getAddressLine1())
                .addressLine2(org.getAddressLine2())
                .city(org.getCity())
                .state(org.getState())
                .postalCode(org.getPostalCode())
                .country(org.getCountry())
                .subscriptionPlan(org.getSubscriptionPlan())
                .subscriptionStatus(org.getSubscriptionStatus())
                .subscriptionStartDate(org.getSubscriptionStartDate())
                .subscriptionEndDate(org.getSubscriptionEndDate())
                .trialEndsAt(org.getTrialEndsAt())
                .maxUsers(org.getMaxUsers())
                .maxStorage(org.getMaxStorage())
                .themeMode(org.getThemeMode())
                .themePrimaryColor(org.getThemePrimaryColor())
                .themeSecondaryColor(org.getThemeSecondaryColor())
                .themeAccentColor(org.getThemeAccentColor())
                .themeSidebarColor(org.getThemeSidebarColor())
                .themeSidebarTextColor(org.getThemeSidebarTextColor())
                .status(org.getStatus())
                .isActive(org.getIsActive())
                .createdAt(org.getCreatedAt())
                .updatedAt(org.getUpdatedAt())
                .createdBy(org.getCreatedBy())
                .updatedBy(org.getUpdatedBy())
                .build();
    }
}

