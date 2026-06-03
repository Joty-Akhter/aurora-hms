package com.easyops.hospitalpharmacy.integration.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class InHouseDispenseFillResponsePayload {
    private UUID prescriptionId;
    private UUID dispenseOrderId;
    private String fillStatus;
    private UUID transmissionId;
    private boolean transmissionUpdated;
}
