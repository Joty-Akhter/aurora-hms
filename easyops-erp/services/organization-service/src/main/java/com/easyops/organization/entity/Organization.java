package com.easyops.organization.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Organization Entity
 * Represents a tenant organization in the multi-tenant ERP system
 */
@Entity
@Table(name = "organizations", schema = "admin")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Organization {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "legal_name", length = 255)
    private String legalName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 500)
    private String logo;

    @Column(length = 255)
    private String website;

    @Column(length = 255)
    private String email;

    @Column(length = 50)
    private String phone;

    // Business Information
    @Column(length = 100)
    private String industry;

    @Column(name = "business_type", length = 100)
    private String businessType;

    @Column(name = "tax_id", length = 100)
    private String taxId;

    @Column(name = "registration_number", length = 100)
    private String registrationNumber;

    @Column(name = "fiscal_year_start")
    private LocalDate fiscalYearStart;

    @Column(length = 3)
    private String currency = "BDT"; // Bangladeshi Taka

    @Column(length = 100)
    private String timezone = "UTC";

    @Column(length = 10)
    private String locale = "en-US";

    // Address Information
    @Column(name = "address_line1", length = 255)
    private String addressLine1;

    @Column(name = "address_line2", length = 255)
    private String addressLine2;

    @Column(length = 100)
    private String city;

    @Column(length = 100)
    private String state;

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @Column(length = 2)
    private String country;

    // Subscription Information
    @Column(name = "subscription_plan", length = 50)
    private String subscriptionPlan = "FREE";

    @Column(name = "subscription_status", length = 50)
    private String subscriptionStatus = "TRIAL";

    @Column(name = "subscription_start_date")
    private ZonedDateTime subscriptionStartDate;

    @Column(name = "subscription_end_date")
    private ZonedDateTime subscriptionEndDate;

    @Column(name = "trial_ends_at")
    private ZonedDateTime trialEndsAt;

    @Column(name = "max_users")
    private Integer maxUsers = 10;

    @Column(name = "max_storage")
    private Long maxStorage = 1073741824L; // 1GB in bytes

    // Theme / Branding
    @Column(name = "theme_mode", length = 10)
    private String themeMode = "light";

    @Column(name = "theme_primary_color", length = 20)
    private String themePrimaryColor = "#2563eb";

    @Column(name = "theme_secondary_color", length = 20)
    private String themeSecondaryColor = "#0891b2";

    @Column(name = "theme_accent_color", length = 20)
    private String themeAccentColor = "#7c3aed";

    @Column(name = "theme_sidebar_color", length = 20)
    private String themeSidebarColor = "#1e293b";

    @Column(name = "theme_sidebar_text_color", length = 20)
    private String themeSidebarTextColor = "#f1f5f9";

    // Status & Metadata
    @Column(length = 50)
    private String status = "ACTIVE";

    @Column(name = "is_active")
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private ZonedDateTime updatedAt;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "updated_by")
    private UUID updatedBy;
}

