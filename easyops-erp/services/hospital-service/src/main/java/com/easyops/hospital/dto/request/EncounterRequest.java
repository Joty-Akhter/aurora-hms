package com.easyops.hospital.dto.request;

import com.easyops.hospital.entity.Encounter;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EncounterRequest {
    
    /**
     * Optional human-readable encounter number. If not provided, the system will generate one.
     */
    private String encounterNumber;
    
    @NotNull(message = "Patient ID is required")
    private UUID patientId;
    
    @NotNull(message = "Encounter type is required")
    private Encounter.EncounterType encounterType;
    
    @NotNull(message = "Start date is required")
    private LocalDate startDate;
    
    @NotNull(message = "Start time is required")
    private LocalTime startTime;
    
    private LocalDate endDate;
    private LocalTime endTime;
    
    private LocalDate admissionDate;
    private LocalTime admissionTime;
    
    private LocalDate dischargeDate;
    private LocalTime dischargeTime;
    
    private Encounter.EncounterStatus status;
    
    private UUID locationId;
    private UUID departmentId;
    private String roomNumber;
    private String bedNumber;
    
    private UUID attendingPhysicianId;
    private UUID admittingPhysicianId;
    private UUID primaryCareProviderId;
    private UUID referringPhysicianId;
    
    private String chiefComplaint;
    private String admissionDiagnosis;
    private String primaryDiagnosis;
    private String[] secondaryDiagnoses;
    private String dischargeDiagnosis;
    private String dischargeDisposition;
    private String dischargeInstructions;
    
    private String visitReason;
    private String visitType;
    private String serviceType;
    
    private UUID insuranceProviderId;
    private String insurancePolicyNumber;
    private String authorizationNumber;
    private String billingStatus;
    
    private String notes;
    private String specialInstructions;
    
    private Boolean isEmergency;
    private Boolean isReadmission;
    private String readmissionReason;
}
