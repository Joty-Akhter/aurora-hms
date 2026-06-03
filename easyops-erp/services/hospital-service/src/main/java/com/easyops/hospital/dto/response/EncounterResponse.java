package com.easyops.hospital.dto.response;

import com.easyops.hospital.entity.Encounter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EncounterResponse {
    
    private UUID encounterId;
    private UUID patientId;
    private String patientName;
    private String mrn;
    private UUID organizationId;
    private String encounterNumber;
    
    private Encounter.EncounterType encounterType;
    private Encounter.EncounterStatus status;
    
    private LocalDate startDate;
    private LocalTime startTime;
    private LocalDate endDate;
    private LocalTime endTime;
    
    private LocalDate admissionDate;
    private LocalTime admissionTime;
    private LocalDate dischargeDate;
    private LocalTime dischargeTime;
    
    private UUID locationId;
    private UUID departmentId;
    private String roomNumber;
    private String bedNumber;
    
    private UUID attendingPhysicianId;
    private String attendingPhysicianName;
    private UUID admittingPhysicianId;
    private String admittingPhysicianName;
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
    
    private Integer lengthOfStayDays;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private UUID createdBy;
    private UUID updatedBy;
}
