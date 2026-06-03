package com.easyops.hospital.dto.response;

import com.easyops.hospital.entity.Patient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientResponse {
    
    private UUID patientId;
    private String mrn;
    private UUID organizationId;
    
    // Personal Identification
    private String fullName;
    private String preferredName;
    private LocalDate dateOfBirth;
    private Patient.Gender gender;
    private String sexAtBirth;
    private String idNo;
    private String idType;
    private String race;
    private String ethnicity;
    private String maritalStatus;
    private String patientType;
    private String fatherName;
    private String motherName;
    private String spouseName;
    private String bloodGroup;
    private String religion;
    private String occupation;
    private String introducedBy;
    
    // Address Information
    private String primaryAddressLine1;
    private String primaryAddressLine2;
    private String primaryCity;
    private String primaryState;
    private String primaryZip;
    private String primaryCountry;
    private String mailingAddressLine1;
    private String mailingAddressLine2;
    private String mailingCity;
    private String mailingState;
    private String mailingZip;
    private String mailingCountry;
    
    // Contact Information
    private String primaryPhone;
    private String primaryPhoneType;
    private String secondaryPhone;
    private String secondaryPhoneType;
    private String primaryEmail;
    private String secondaryEmail;
    private Patient.PreferredContactMethod preferredContactMethod;
    private Boolean consentTextMessaging;
    private Boolean consentEmailCommunication;
    
    // Clinical Assignment
    private UUID primaryCareProviderId;
    private UUID primaryCareLocationId;
    private UUID referringPhysicianId;
    private Patient.PatientStatus patientStatus;
    
    // Clinical Information
    private String preferredLanguage;
    private Boolean interpreterNeeded;
    private String specialNeeds;
    
    // Administrative
    private LocalDateTime registrationDate;
    private UUID registeredBy;
    private UUID registrationLocationId;
    
    // Audit Fields
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private UUID createdBy;
    private UUID updatedBy;
    
    // Related Entities
    private List<EmergencyContactResponse> emergencyContacts;
    private List<InsuranceResponse> insuranceList;
    private List<ConsentResponse> consents;

    // Patient identity card (Hospital Card Service — PATIENT_IDENTITY); optional on create/update responses
    private UUID identityCardId;
    private String identityCardNumber;
    /** ISSUED, SKIPPED, FAILED, DISABLED — see patient-identity-card-implementation-plan.md */
    private String identityCardStatus;
    private String identityCardMessage;
}
