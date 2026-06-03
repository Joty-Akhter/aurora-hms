package com.easyops.hospitalpharmacy.dto.request;

import lombok.Data;

import java.util.UUID;

/** Phase P3 WS-E — partial update of paper Rx / external validation fields. */
@Data
public class PatchDispenseOrderRegionalRequest {

    private String paperPrescriptionRef;

    private UUID prescriptionImageAttachmentId;

    /**
     * When true, clears {@code prescription_image_attachment_id}. Ignored if {@link #prescriptionImageAttachmentId} is non-null.
     */
    private Boolean clearPrescriptionImageAttachment;

    /** NOT_REQUIRED | PENDING | VERIFIED | FAILED_SOFT */
    private String externalValidationStatus;
}
