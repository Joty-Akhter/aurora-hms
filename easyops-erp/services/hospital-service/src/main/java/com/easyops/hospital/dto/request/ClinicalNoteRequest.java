package com.easyops.hospital.dto.request;

import com.easyops.hospital.entity.ClinicalNote;
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
public class ClinicalNoteRequest {
    
    @NotNull(message = "Patient ID is required")
    private UUID patientId;
    
    private UUID encounterId;
    
    @NotNull(message = "Note type is required")
    private ClinicalNote.NoteType noteType;
    
    @NotNull(message = "Note date is required")
    private LocalDate noteDate;
    
    @NotNull(message = "Note time is required")
    private LocalTime noteTime;
    
    // SOAP Note Components
    private String subjective;
    private String objective;
    private String assessment;
    private String plan;
    
    // Additional Note Fields
    private String chiefComplaint;
    private String reviewOfSystems;
    private String physicalExamination;
    private String clinicalImpression;
    private String treatmentPlan;
    private String followUpInstructions;
    
    // Note Status
    private ClinicalNote.NoteStatus noteStatus;
    
    // Additional Metadata
    private String specialty;
    private UUID departmentId;
    private UUID locationId;
    private String visitType;
    private String notes;
    
    // Template ID (if creating from template)
    private UUID templateId;
}
