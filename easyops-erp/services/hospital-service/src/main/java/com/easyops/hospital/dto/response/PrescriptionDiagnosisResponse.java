package com.easyops.hospital.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * FR-P1.4a: One ICD-10 diagnosis entry in a prescription response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrescriptionDiagnosisResponse {

    private UUID id;
    private String diagnosisCode;
    private String diagnosisDescription;
    private Boolean isPrimary;
    private Integer sequenceOrder;
    private LocalDateTime createdAt;
}
