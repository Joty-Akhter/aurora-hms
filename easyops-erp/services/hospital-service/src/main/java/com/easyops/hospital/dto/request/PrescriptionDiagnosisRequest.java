package com.easyops.hospital.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * FR-P1.4a: One ICD-10 diagnosis entry within a prescription request.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrescriptionDiagnosisRequest {

    @NotBlank(message = "Diagnosis code is required")
    @Size(max = 20, message = "Diagnosis code must be at most 20 characters")
    private String diagnosisCode;

    @Size(max = 500, message = "Diagnosis description must be at most 500 characters")
    private String diagnosisDescription;

    /** Whether this is the primary/principal diagnosis for the prescription. */
    private Boolean isPrimary;

    /**
     * Optional display / sequencing hint (lower sorts first). When any entry includes a non-null
     * value, the list is ordered by this field (entries with null sort after those with a value)
     * before FR-P1.4a validation and before assigning {@code sequenceOrder} on persist.
     * When all are null, request list order is preserved.
     */
    private Integer sequenceOrder;
}
