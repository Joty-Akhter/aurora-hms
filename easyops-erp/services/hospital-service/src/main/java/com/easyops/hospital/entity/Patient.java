package com.easyops.hospital.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "patients", schema = "ehr")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Patient {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "patient_id")
    private UUID patientId;
    
    @Column(name = "mrn", nullable = false, unique = true, length = 50)
    private String mrn;
    
    @Column(name = "organization_id")
    private UUID organizationId;
    
    // Personal Identification (single stored name)
    @Column(name = "full_name", nullable = false, length = 255)
    private String fullName;
    
    @Column(name = "preferred_name", length = 100)
    private String preferredName;
    
    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;
    
    @Column(name = "gender", length = 20)
    @Enumerated(EnumType.STRING)
    private Gender gender;
    
    @Column(name = "sex_at_birth", length = 20)
    private String sexAtBirth;
    
    @Column(name = "id_no", length = 20)
    private String idNo;

    /** Document category for {@link #idNo} (e.g. NID, PASSPORT). */
    @Column(name = "id_type", length = 50)
    private String idType;
    
    @Column(name = "race", length = 50)
    private String race;
    
    @Column(name = "ethnicity", length = 50)
    private String ethnicity;
    
    @Column(name = "marital_status", length = 20)
    private String maritalStatus;

    /** Registration category (General, Corporate, Insurance, Staff, etc.). */
    @Column(name = "patient_type", length = 50)
    private String patientType;

    @Column(name = "father_name", length = 200)
    private String fatherName;

    @Column(name = "mother_name", length = 200)
    private String motherName;

    @Column(name = "spouse_name", length = 200)
    private String spouseName;

    @Column(name = "blood_group", length = 20)
    private String bloodGroup;

    @Column(name = "religion", length = 100)
    private String religion;

    @Column(name = "occupation", length = 150)
    private String occupation;

    @Column(name = "introduced_by", length = 255)
    private String introducedBy;
    
    // Contact Information - Primary Address
    @Column(name = "primary_address_line1", length = 255)
    private String primaryAddressLine1;
    
    @Column(name = "primary_address_line2", length = 255)
    private String primaryAddressLine2;
    
    @Column(name = "primary_city", length = 100)
    private String primaryCity;
    
    @Column(name = "primary_state", length = 50)
    private String primaryState;
    
    @Column(name = "primary_zip", length = 20)
    private String primaryZip;
    
    @Column(name = "primary_country", length = 50)
    @Builder.Default
    private String primaryCountry = "Bangladesh";
    
    // Mailing Address
    @Column(name = "mailing_address_line1", length = 255)
    private String mailingAddressLine1;
    
    @Column(name = "mailing_address_line2", length = 255)
    private String mailingAddressLine2;
    
    @Column(name = "mailing_city", length = 100)
    private String mailingCity;
    
    @Column(name = "mailing_state", length = 50)
    private String mailingState;
    
    @Column(name = "mailing_zip", length = 20)
    private String mailingZip;
    
    @Column(name = "mailing_country", length = 50)
    private String mailingCountry;
    
    // Phone Numbers
    @Column(name = "primary_phone", length = 50)
    private String primaryPhone;
    
    @Column(name = "primary_phone_type", length = 20)
    private String primaryPhoneType;
    
    @Column(name = "secondary_phone", length = 50)
    private String secondaryPhone;
    
    @Column(name = "secondary_phone_type", length = 20)
    private String secondaryPhoneType;
    
    // Email
    @Column(name = "primary_email", length = 255)
    private String primaryEmail;
    
    @Column(name = "secondary_email", length = 255)
    private String secondaryEmail;
    
    @Column(name = "preferred_contact_method", length = 20)
    @Enumerated(EnumType.STRING)
    private PreferredContactMethod preferredContactMethod;
    
    @Column(name = "consent_text_messaging")
    @Builder.Default
    private Boolean consentTextMessaging = true;
    
    @Column(name = "consent_email_communication")
    @Builder.Default
    private Boolean consentEmailCommunication = false;
    
    // Clinical Assignment
    @Column(name = "primary_care_provider_id")
    private UUID primaryCareProviderId;
    
    @Column(name = "primary_care_location_id")
    private UUID primaryCareLocationId;
    
    @Column(name = "referring_physician_id")
    private UUID referringPhysicianId;
    
    @Column(name = "patient_status", length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private PatientStatus patientStatus = PatientStatus.ACTIVE;
    
    // Clinical Information
    @Column(name = "preferred_language", length = 50)
    @Builder.Default
    private String preferredLanguage = "English";
    
    @Column(name = "interpreter_needed")
    @Builder.Default
    private Boolean interpreterNeeded = false;
    
    @Column(name = "special_needs", columnDefinition = "TEXT")
    private String specialNeeds;
    
    // Administrative Information
    @Column(name = "registration_date")
    @Builder.Default
    private LocalDateTime registrationDate = LocalDateTime.now();
    
    @Column(name = "registered_by")
    private UUID registeredBy;
    
    @Column(name = "registration_location_id")
    private UUID registrationLocationId;
    
    // Audit Fields
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "created_by")
    private UUID createdBy;
    
    @Column(name = "updated_by")
    private UUID updatedBy;
    
    // Relationships
    @OneToMany(mappedBy = "patient", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PatientEmergencyContact> emergencyContacts;
    
    @OneToMany(mappedBy = "patient", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PatientInsurance> insuranceList;
    
    @OneToMany(mappedBy = "patient", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PatientConsent> consents;
    
    public enum Gender {
        Male, Female, Other, Prefer_not_to_answer
    }
    
    public enum PreferredContactMethod {
        Phone, Email, Mail, Text_Message
    }
    
    public enum PatientStatus {
        ACTIVE, INACTIVE, DECEASED, ARCHIVED
    }
}
