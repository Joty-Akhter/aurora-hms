package com.easyops.hospital.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HL7MessageResponse {
    
    private String messageType; // "HL7_V2_ORM" or "HL7_FHIR_SERVICEREQUEST"
    private String messageFormat; // "TEXT" or "JSON"
    private String messageContent; // For HL7 V2 (text format)
    private Map<String, Object> messageResource; // For FHIR (JSON format)
    private String orderId;
    private String orderNumber;
}
