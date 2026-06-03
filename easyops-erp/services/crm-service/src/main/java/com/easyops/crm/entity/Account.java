package com.easyops.crm.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "accounts", schema = "crm")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Account {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "account_id")
    private UUID accountId;
    
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;
    
    @Column(name = "account_number", nullable = false, length = 50)
    private String accountNumber;
    
    // Basic Info
    @Column(name = "account_name", nullable = false, length = 255)
    private String accountName;
    
    @Column(name = "account_type", length = 50)
    private String accountType; // CUSTOMER, PROSPECT, PARTNER, COMPETITOR
    
    @Column(name = "industry", length = 100)
    private String industry;
    
    // Contact Info
    @Column(name = "phone", length = 50)
    private String phone;
    
    @Column(name = "fax", length = 50)
    private String fax;
    
    @Column(name = "email", length = 255)
    private String email;
    
    @Column(name = "website", length = 255)
    private String website;
    
    // Billing Address
    @Column(name = "billing_street", length = 255)
    private String billingStreet;
    
    @Column(name = "billing_city", length = 100)
    private String billingCity;
    
    @Column(name = "billing_state", length = 100)
    private String billingState;
    
    @Column(name = "billing_postal_code", length = 20)
    private String billingPostalCode;
    
    @Column(name = "billing_country", length = 100)
    private String billingCountry;
    
    // Shipping Address
    @Column(name = "shipping_street", length = 255)
    private String shippingStreet;
    
    @Column(name = "shipping_city", length = 100)
    private String shippingCity;
    
    @Column(name = "shipping_state", length = 100)
    private String shippingState;
    
    @Column(name = "shipping_postal_code", length = 20)
    private String shippingPostalCode;
    
    @Column(name = "shipping_country", length = 100)
    private String shippingCountry;
    
    // Business Info
    @Column(name = "company_size", length = 50)
    private String companySize;
    
    @Column(name = "annual_revenue", precision = 15, scale = 2)
    private BigDecimal annualRevenue;
    
    @Column(name = "number_of_employees")
    private Integer numberOfEmployees;
    
    // Relationship
    @Column(name = "parent_account_id")
    private UUID parentAccountId;
    
    @Column(name = "owner_id")
    private UUID ownerId; // Account manager
    
    @Column(name = "rating", length = 20)
    private String rating; // HOT, WARM, COLD
    
    // Status
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    @Column(name = "customer_since")
    private LocalDate customerSince;
    
    // Tax & Legal
    @Column(name = "tax_id", length = 100)
    private String taxId;
    
    @Column(name = "registration_number", length = 100)
    private String registrationNumber;
    
    // Additional
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
    
    @Column(name = "tags", columnDefinition = "TEXT[]")
    private String[] tags;
    
    // Audit
    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private UUID createdBy;
    
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedBy
    @Column(name = "updated_by")
    private UUID updatedBy;
    
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}


