package com.easyops.hospitalpharmacy.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateDispenseOrderRequest {

    /** Optional; UUID string, or omit. */
    private String prescriptionId;

    @NotNull
    private UUID pharmacyLocationId;

    /** Optional; UUID string or encounter number (resolved via hospital-service). */
    private String visitId;

    /** Optional; UUID string or MRN (e.g. HOSP-2026-000001), resolved via hospital-service. */
    private String patientId;

    /** Optional; UUID string only. */
    private String departmentId;

    @NotNull
    private String contextType; // PATIENT_PRESCRIPTION, WALK_IN, DEPARTMENT_ISSUE

    /** Phase P3 WS-E — paper prescription identifier when no EHR Rx is linked. */
    private String paperPrescriptionRef;

    private UUID prescriptionImageAttachmentId;

    /** NOT_REQUIRED | PENDING | VERIFIED | FAILED_SOFT */
    private String externalValidationStatus;
}
