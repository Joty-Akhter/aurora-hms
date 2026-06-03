package com.easyops.hospital.dto.response;

import com.easyops.hospital.entity.PrescriptionTransmission;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrescriptionTransmissionResponse {
    
    private UUID transmissionId;
    private UUID prescriptionId;
    
    private LocalDateTime transmissionDate;
    private PrescriptionTransmission.TransmissionStatus transmissionStatus;
    private PrescriptionTransmission.TransmissionMethod transmissionMethod;
    
    private String networkName;
    private String networkId;
    private String networkTransactionId;
    
    // Pharmacy Information
    private UUID pharmacyId;
    private String pharmacyName;
    private String pharmacyNpi;
    private String pharmacyDea;
    private String pharmacyAddressLine1;
    private String pharmacyAddressLine2;
    private String pharmacyCity;
    private String pharmacyState;
    private String pharmacyZip;
    private String pharmacyPhone;
    private String pharmacyFax;
    
    // Transmission Results
    private Boolean transmissionSuccess;
    private Boolean confirmationReceived;
    private LocalDateTime confirmationDate;
    private String confirmationMessage;
    private String errorMessage;
    private String errorCode;
    private Integer retryCount;
    private LocalDateTime lastRetryDate;
    private Integer maxRetries;
    
    // Network Response Data
    private Map<String, Object> networkResponse;
    private Map<String, Object> transmissionPayload;
    
    // Fill Status Information
    private PrescriptionTransmission.FillStatus fillStatus;
    private LocalDateTime fillStatusDate;
    private String fillStatusMessage;
    private LocalDateTime filledDate;
    private LocalDateTime pickedUpDate;
    private Boolean cancelledByPharmacy;
    private String cancellationReason;
    
    // Provider Information
    private UUID transmittedBy;
    private String transmittedByName;
    private String transmittedByNpi;
    
    // Audit Fields
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private UUID createdBy;
    private UUID updatedBy;
}
