package com.easyops.hospital.dto.response;

import com.easyops.hospital.entity.FormularyCheck;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FormularyCheckResponse {
    
    private UUID formularyCheckId;
    private UUID prescriptionId;
    private UUID insuranceId;
    private String insuranceCompanyName;
    private String policyNumber;
    private String medicationCode;
    private String medicationName;
    private FormularyCheck.CoverageStatus coverageStatus;
    private String formularyTier;
    private Boolean requiresPriorAuthorization;
    private Boolean priorAuthorizationRequired;
    private Boolean stepTherapyRequired;
    private Integer quantityLimit;
    private Integer daysSupplyLimit;
    private BigDecimal copayAmount;
    private BigDecimal coinsurancePercentage;
    private Boolean deductibleApplies;
    private BigDecimal patientCostEstimate;
    private BigDecimal insurancePays;
    private String pbmName;
    private String pbmId;
    private String formularyId;
    private String formularyName;
    private LocalDateTime checkDate;
    private FormularyCheck.CheckStatus checkStatus;
    private String errorMessage;
    
    private List<FormularyAlternativeResponse> alternatives;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FormularyAlternativeResponse {
        private UUID alternativeId;
        private String medicationCode;
        private String medicationName;
        private String genericName;
        private String formularyTier;
        private FormularyCheck.CoverageStatus coverageStatus;
        private Boolean requiresPriorAuthorization;
        private BigDecimal copayAmount;
        private BigDecimal patientCostEstimate;
        private String alternativeType;
        private String reason;
        private Boolean isPreferred;
        private Integer rank;
    }
}
