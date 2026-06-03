package com.easyops.hospital.dto.response;

import com.easyops.hospital.entity.ImagingOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImagingOrderResponse {
    
    private UUID orderId;
    private UUID patientId;
    private String patientName;
    private String mrn;
    private UUID encounterId;
    private UUID organizationId;
    
    private String orderNumber;
    private LocalDateTime orderDate;
    private LocalDateTime scheduledDate;
    private LocalTime scheduledTime;
    
    private UUID orderingProviderId;
    private String orderingProviderName;
    private UUID orderingFacilityId;
    private String orderingFacilityName;
    
    private String studyType;
    private ImagingOrder.StudyModality studyModality;
    private String studyDescription;
    private String cptCode;
    private String bodyPart;
    private String laterality;
    private String specificAnatomicalSite;
    private String viewProjection;
    
    private String clinicalIndication;
    private ImagingOrder.OrderPriority priority;
    private String specialInstructions;
    private Boolean contrastRequired;
    private String contrastType;
    private Boolean patientPreparationRequired;
    private String patientPreparationInstructions;
    private Boolean sedationRequired;
    
    private ImagingOrder.OrderStatus orderStatus;
    private LocalDateTime sentDate;
    private LocalDateTime cancelledDate;
    private String cancellationReason;
    private Boolean noShow;
    
    private String transmissionMethod;
    private String transmissionStatus;
    private LocalDateTime transmissionDate;
    private UUID radiologyFacilityId;
    private String radiologyFacilityName;
    private Boolean orderConfirmationReceived;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private UUID createdBy;
    private UUID updatedBy;
}
