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
public class PatientSummaryResponse {
    
    private UUID patientId;
    private String patientName;
    private String mrn;
    private LocalDate dateOfBirth;
    private Integer age;
    private com.easyops.hospital.entity.Patient.Gender gender;
    
    // Active Problems
    private Integer activeProblemsCount;
    private List<ProblemSummary> activeProblems;
    
    // Active Prescriptions
    private Integer activePrescriptionsCount;
    private List<PrescriptionSummary> activePrescriptions;
    
    // Active Allergies
    private Integer activeAllergiesCount;
    private List<AllergySummary> activeAllergies;
    
    // Latest Vital Signs
    private VitalSignsSummary latestVitalSigns;
    
    // Recent Notes
    private Integer recentNotesCount;
    private List<NoteSummary> recentNotes;
    
    // Recent Immunizations
    private Integer recentImmunizationsCount;
    private List<ImmunizationSummary> recentImmunizations;
    
    // Active Medications
    private Integer activeMedicationsCount;
    private List<MedicationSummary> activeMedications;
    
    private LocalDateTime lastUpdated;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProblemSummary {
        private UUID problemId;
        private String problemName;
        private String problemType;
        private String status;
        private String icd10Code;
        private LocalDate onsetDate;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PrescriptionSummary {
        private UUID prescriptionId;
        private String medicationName;
        private String dosage;
        private String route;
        private String frequency;
        private LocalDate startDate;
        private Integer refillsRemaining;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AllergySummary {
        private UUID allergyId;
        private String allergenName;
        private String allergenType;
        private String severity;
        private String reactionType;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VitalSignsSummary {
        private LocalDate measurementDate;
        private java.time.LocalTime measurementTime;
        private Integer systolicBp;
        private Integer diastolicBp;
        private Integer heartRate;
        private java.math.BigDecimal temperature;
        private String temperatureUnit;
        private java.math.BigDecimal oxygenSaturation;
        private java.math.BigDecimal weight;
        private String weightUnit;
        private java.math.BigDecimal bmi;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NoteSummary {
        private UUID noteId;
        private String noteType;
        private LocalDate noteDate;
        private java.time.LocalTime noteTime;
        private String chiefComplaint;
        private String noteStatus;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImmunizationSummary {
        private UUID immunizationId;
        private String vaccineName;
        private LocalDate administrationDate;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MedicationSummary {
        private UUID medicationId;
        private String medicationName;
        private String genericName;
        private String dosage;
        private String route;
        private String frequency;
        private LocalDate startDate;
        private String indication;
        private String medicationStatus;
    }
}
