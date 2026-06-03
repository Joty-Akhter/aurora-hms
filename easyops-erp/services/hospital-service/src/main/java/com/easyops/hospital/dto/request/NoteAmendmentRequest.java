package com.easyops.hospital.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoteAmendmentRequest {
    
    @NotNull(message = "Amendment reason is required")
    private String amendmentReason;
    
    // Updated note content
    private String subjective;
    private String objective;
    private String assessment;
    private String plan;
    private String chiefComplaint;
    private String reviewOfSystems;
    private String physicalExamination;
    private String clinicalImpression;
    private String treatmentPlan;
    private String followUpInstructions;
    private String notes;
}
