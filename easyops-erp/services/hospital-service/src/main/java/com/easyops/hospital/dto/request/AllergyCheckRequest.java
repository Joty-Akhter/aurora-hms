package com.easyops.hospital.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AllergyCheckRequest {
    
    private String medicationCode; // RxNorm or NDC
    private String medicationName;
    private UUID patientId;
}
