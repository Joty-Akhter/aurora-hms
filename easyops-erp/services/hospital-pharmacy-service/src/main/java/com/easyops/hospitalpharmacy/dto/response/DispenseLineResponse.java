package com.easyops.hospitalpharmacy.dto.response;

import com.easyops.hospitalpharmacy.entity.DispenseLine;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class DispenseLineResponse {

    private UUID id;
    private UUID dispenseOrderId;
    private UUID prescriptionLineId;
    private UUID drugId;
    private String drugGenericName;
    private String drugBrandName;
    private String batchNumber;
    private BigDecimal quantityPrescribed;
    private BigDecimal quantityDispensed;
    private BigDecimal quantityReturned;
    private DispenseLine.Status status;
    private String reasonCode;
    private UUID documentingUserId;
    private String overrideReasonCode;
    private UUID substitutedDrugId;
    private String formularyOverrideReason;
    private UUID overrideApproverId;
    private UUID witnessUserId;
    private String clinicalSafetyOverrideReason;
    private BigDecimal remainingQuantity;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}

