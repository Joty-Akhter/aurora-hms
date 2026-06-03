package com.easyops.hospital.dto.request;

import com.easyops.hospital.entity.ImagingOrder;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class ImagingOrderRequest {
    
    @NotNull(message = "Patient ID is required")
    private UUID patientId;
    
    private UUID encounterId;
    
    private String orderNumber;
    
    @NotBlank(message = "Study type is required")
    private String studyType;
    
    @NotNull(message = "Study modality is required")
    private ImagingOrder.StudyModality studyModality;
    
    @NotBlank(message = "Study description is required")
    private String studyDescription;
    
    @NotBlank(message = "CPT code is required")
    private String cptCode;
    
    @NotBlank(message = "Body part is required")
    private String bodyPart;
    
    private String laterality;
    
    private String specificAnatomicalSite;
    
    private String viewProjection;
    
    @NotBlank(message = "Clinical indication is required")
    private String clinicalIndication;
    
    private ImagingOrder.OrderPriority priority = ImagingOrder.OrderPriority.ROUTINE;
    
    private String specialInstructions;
    
    private Boolean contrastRequired = false;
    
    private String contrastType;
    
    private Boolean patientPreparationRequired = false;
    
    private String patientPreparationInstructions;
    
    private Boolean sedationRequired = false;
    
    private UUID orderingProviderId;
    
    private String orderingProviderName;
    
    private UUID orderingFacilityId;
    
    private String orderingFacilityName;
    
    private LocalDateTime scheduledDate;
    
    private LocalTime scheduledTime;
    
    private UUID radiologyFacilityId;
    
    private String radiologyFacilityName;
}
