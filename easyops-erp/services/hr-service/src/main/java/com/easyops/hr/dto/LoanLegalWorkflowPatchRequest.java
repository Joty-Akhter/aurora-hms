package com.easyops.hr.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** ST-04: update legal workflow label on a loan (not a full task engine). */
@Data
public class LoanLegalWorkflowPatchRequest {

    @NotBlank
    @Size(max = 40)
    private String legalWorkflowStatus;
}
