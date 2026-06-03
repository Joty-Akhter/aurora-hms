package com.easyops.hospitalpharmacy.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class FormularyRuleResponse {

    private UUID id;
    private UUID drugId;
    private boolean restricted;
    private String restrictionReason;
    private UUID wardId;
    private UUID departmentId;
    private UUID corporateContractId;
    private List<UUID> preferredAlternativeDrugIds;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}

