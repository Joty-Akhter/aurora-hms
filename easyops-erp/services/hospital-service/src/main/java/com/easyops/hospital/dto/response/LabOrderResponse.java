package com.easyops.hospital.dto.response;

import com.easyops.hospital.entity.LabOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LabOrderResponse {
    
    private UUID orderId;
    private UUID patientId;
    private String patientName;
    private String mrn;
    private UUID encounterId;
    private UUID organizationId;
    
    private String orderNumber;
    private LocalDateTime orderDate;
    private LocalDateTime scheduledDate;
    
    private UUID orderingProviderId;
    private String orderingProviderName;
    private UUID orderingFacilityId;
    private String orderingFacilityName;
    
    private String testName;
    private String loincCode;
    private String testCategory;
    private String testType;
    private Boolean isTestPanel;
    private String panelName;
    
    private String clinicalIndication;
    private LabOrder.OrderPriority priority;
    private String specialInstructions;
    private Boolean fastingRequired;
    private String patientPreparationInstructions;
    
    private LabOrder.OrderStatus orderStatus;
    private LocalDateTime sentDate;
    private LocalDateTime collectedDate;
    private LocalDateTime cancelledDate;
    private String cancellationReason;
    
    private String transmissionMethod;
    private String transmissionStatus;
    private LocalDateTime transmissionDate;
    private UUID laboratoryId;
    private String laboratoryName;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private UUID createdBy;
    private UUID updatedBy;
}
