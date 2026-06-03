package com.easyops.hospitalpharmacy.dto.request;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class FormularyRuleRequest {

    private Boolean restricted;

    private String restrictionReason;

    private UUID wardId;

    private UUID departmentId;

    private UUID corporateContractId;

    private List<UUID> preferredAlternativeDrugIds;
}

