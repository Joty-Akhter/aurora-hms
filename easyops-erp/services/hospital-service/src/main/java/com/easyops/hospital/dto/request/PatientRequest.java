package com.easyops.hospital.dto.request;

import com.easyops.hospital.entity.Patient;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientRequest {
    
    @NotBlank(message = "Full name is required")
    private String fullName;

    private String preferredName;

    /** Required unless {@link #ageYears} is provided (quick registration from appointment scheduling). */
    private LocalDate dateOfBirth;

    /** Approximate age in years; used to derive dateOfBirth when DOB is omitted. */
    @Min(0)
    @Max(150)
    private Integer ageYears;
    
    private Patient.Gender gender;
    private String sexAtBirth;
    private String idNo;
    /** Document type for {@code idNo} (e.g. NID, PASSPORT). */
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
    
    // Primary Address
    private String primaryAddressLine1;
    private String primaryAddressLine2;
    private String primaryCity;
    private String primaryState;
    private String primaryZip;
    private String primaryCountry;
    
    // Mailing Address
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
    
    @Email(message = "Invalid email format")
    private String primaryEmail;
    
    @Email(message = "Invalid email format")
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
    private UUID registeredBy;
    private UUID registrationLocationId;
    private UUID organizationId;

    /** Optional: exclude this patient from duplicate checks (edit / update flows). */
    private UUID excludePatientId;
    
    @AssertTrue(message = "Date of birth is required (or provide age in years)")
    public boolean isDateOfBirthOrAgeProvided() {
        return dateOfBirth != null || (ageYears != null && ageYears >= 0);
    }

    // Related Entities
    private List<EmergencyContactRequest> emergencyContacts;
    private List<InsuranceRequest> insuranceList;
    private List<ConsentRequest> consents;
}
