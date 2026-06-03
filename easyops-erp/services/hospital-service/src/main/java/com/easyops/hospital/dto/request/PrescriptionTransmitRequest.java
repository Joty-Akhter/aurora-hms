package com.easyops.hospital.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrescriptionTransmitRequest {
    
    private Boolean overrideInteractions;
    private Boolean overrideAllergies;
    private Boolean overridePdmpCheck;
    private String overrideReason;
    private String pharmacyId;
    private String pharmacyNpi;
    private String pharmacyName;
}
