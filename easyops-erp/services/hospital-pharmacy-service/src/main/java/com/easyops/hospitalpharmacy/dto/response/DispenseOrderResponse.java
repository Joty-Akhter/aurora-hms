package com.easyops.hospitalpharmacy.dto.response;

import com.easyops.hospitalpharmacy.entity.DispenseOrder;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class DispenseOrderResponse {

    private UUID id;
    private UUID prescriptionId;
    private UUID visitId;
    private UUID patientId;
    private UUID pharmacyLocationId;
    private String pharmacyLocationName;
    private DispenseOrder.Status status;
    private DispenseOrder.ContextType contextType;
    private UUID departmentId;
    private OffsetDateTime createdAt;
    private OffsetDateTime completedAt;
    private List<DispenseLineResponse> lines;

    /** Phase P3 WS-E */
    private String paperPrescriptionRef;
    private UUID prescriptionImageAttachmentId;
    private String externalValidationStatus;
}

