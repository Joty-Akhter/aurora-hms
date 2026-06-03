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
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "encounters", schema = "ehr")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Encounter {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "encounter_id")
    private UUID encounterId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;
    
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;
    
    @Column(name = "encounter_number", nullable = false, unique = true, length = 50)
    private String encounterNumber;
    
    // Encounter Classification
    @Column(name = "encounter_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private EncounterType encounterType;
    
    @Column(name = "status", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private EncounterStatus status = EncounterStatus.PLANNED;
    
    // Date and Time Information
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;
    
    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;
    
    @Column(name = "end_date")
    private LocalDate endDate;
    
    @Column(name = "end_time")
    private LocalTime endTime;
    
    @Column(name = "admission_date")
    private LocalDate admissionDate;
    
    @Column(name = "admission_time")
    private LocalTime admissionTime;
    
    @Column(name = "discharge_date")
    private LocalDate dischargeDate;
    
    @Column(name = "discharge_time")
    private LocalTime dischargeTime;
    
    // Location and Department
    @Column(name = "location_id")
    private UUID locationId;
    
    @Column(name = "department_id")
    private UUID departmentId;
    
    @Column(name = "room_number", length = 50)
    private String roomNumber;
    
    @Column(name = "bed_number", length = 50)
    private String bedNumber;
    
    // Provider Information
    @Column(name = "attending_physician_id")
    private UUID attendingPhysicianId;
    
    @Column(name = "admitting_physician_id")
    private UUID admittingPhysicianId;
    
    @Column(name = "primary_care_provider_id")
    private UUID primaryCareProviderId;
    
    @Column(name = "referring_physician_id")
    private UUID referringPhysicianId;
    
    // Clinical Information
    @Column(name = "chief_complaint", columnDefinition = "TEXT")
    private String chiefComplaint;
    
    @Column(name = "admission_diagnosis", columnDefinition = "TEXT")
    private String admissionDiagnosis;
    
    @Column(name = "primary_diagnosis", columnDefinition = "TEXT")
    private String primaryDiagnosis;
    
    @Column(name = "secondary_diagnoses", columnDefinition = "TEXT[]")
    private String[] secondaryDiagnoses;
    
    @Column(name = "discharge_diagnosis", columnDefinition = "TEXT")
    private String dischargeDiagnosis;
    
    @Column(name = "discharge_disposition", length = 100)
    private String dischargeDisposition;
    
    @Column(name = "discharge_instructions", columnDefinition = "TEXT")
    private String dischargeInstructions;
    
    // Visit Details
    @Column(name = "visit_reason", columnDefinition = "TEXT")
    private String visitReason;
    
    @Column(name = "visit_type", length = 50)
    private String visitType;
    
    @Column(name = "service_type", length = 50)
    private String serviceType;
    
    // Insurance and Billing
    @Column(name = "insurance_provider_id")
    private UUID insuranceProviderId;
    
    @Column(name = "insurance_policy_number", length = 100)
    private String insurancePolicyNumber;
    
    @Column(name = "authorization_number", length = 100)
    private String authorizationNumber;
    
    @Column(name = "billing_status", length = 50)
    private String billingStatus;
    
    // Additional Information
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
    
    @Column(name = "special_instructions", columnDefinition = "TEXT")
    private String specialInstructions;
    
    @Column(name = "is_emergency")
    @Builder.Default
    private Boolean isEmergency = false;
    
    @Column(name = "is_readmission")
    @Builder.Default
    private Boolean isReadmission = false;
    
    @Column(name = "readmission_reason", columnDefinition = "TEXT")
    private String readmissionReason;
    
    // Length of Stay (calculated)
    @Column(name = "length_of_stay_days")
    private Integer lengthOfStayDays;
    
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
    
    // Enums
    public enum EncounterType {
        OFFICE_VISIT,
        HOSPITAL_ADMISSION,
        EMERGENCY,
        OUTPATIENT,
        INPATIENT,
        OBSERVATION,
        SURGERY,
        CONSULTATION,
        TELEHEALTH,
        HOME_VISIT,
        URGENT_CARE,
        AMBULATORY,
        OTHER
    }
    
    public enum EncounterStatus {
        PLANNED,
        ARRIVED,
        IN_PROGRESS,
        COMPLETED,
        DISCHARGED,
        CANCELLED,
        NO_SHOW,
        LEFT_WITHOUT_BEING_SEEN,
        ADMITTED,
        TRANSFERRED
    }
}
