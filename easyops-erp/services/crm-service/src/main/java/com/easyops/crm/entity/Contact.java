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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "contacts", schema = "crm")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Contact {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "contact_id")
    private UUID contactId;
    
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;
    
    @Column(name = "account_id")
    private UUID accountId;
    
    // Basic Info
    @Column(name = "first_name", length = 100)
    private String firstName;
    
    @Column(name = "last_name", length = 100)
    private String lastName;
    
    @Column(name = "salutation", length = 20)
    private String salutation; // Mr., Ms., Dr., etc.
    
    @Column(name = "job_title", length = 100)
    private String jobTitle;
    
    @Column(name = "department", length = 100)
    private String department;
    
    // Contact Info
    @Column(name = "email", length = 255)
    private String email;
    
    @Column(name = "phone", length = 50)
    private String phone;
    
    @Column(name = "mobile", length = 50)
    private String mobile;
    
    // Address
    @Column(name = "street", length = 255)
    private String street;
    
    @Column(name = "city", length = 100)
    private String city;
    
    @Column(name = "state", length = 100)
    private String state;
    
    @Column(name = "postal_code", length = 20)
    private String postalCode;
    
    @Column(name = "country", length = 100)
    private String country;
    
    // Social
    @Column(name = "linkedin_url", length = 255)
    private String linkedinUrl;
    
    @Column(name = "twitter_handle", length = 100)
    private String twitterHandle;
    
    // Relationship
    @Column(name = "reports_to_id")
    private UUID reportsToId;
    
    @Column(name = "is_primary")
    private Boolean isPrimary = false;
    
    // Preferences
    @Column(name = "preferred_contact_method", length = 50)
    private String preferredContactMethod; // EMAIL, PHONE, MOBILE
    
    @Column(name = "do_not_email")
    private Boolean doNotEmail = false;
    
    @Column(name = "do_not_call")
    private Boolean doNotCall = false;
    
    // Status
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    // Additional
    @Column(name = "birthdate")
    private LocalDate birthdate;
    
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


