package com.easyops.hospital.dto.request;

import com.easyops.hospital.entity.LabOrder;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LabOrderRequest {
    
    @NotNull(message = "Patient ID is required")
    private UUID patientId;
    
    private UUID encounterId;
    
    private String orderNumber;
    
    @NotBlank(message = "Test name is required")
    private String testName;
    
    private String loincCode;
    private String testCategory;
    private String testType;
    
    private Boolean isTestPanel = false;
    private String panelName;
    
    private String clinicalIndication;
    
    private LabOrder.OrderPriority priority = LabOrder.OrderPriority.ROUTINE;
    
    private String specialInstructions;
    
    private Boolean fastingRequired = false;
    
    private String patientPreparationInstructions;
    
    private UUID orderingProviderId;
    private String orderingProviderName;
    
    private UUID orderingFacilityId;
    private String orderingFacilityName;
    
    private UUID laboratoryId;
    private String laboratoryName;
}
