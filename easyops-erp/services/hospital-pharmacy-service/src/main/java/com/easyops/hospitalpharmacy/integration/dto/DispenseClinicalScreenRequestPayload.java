package com.easyops.hospitalpharmacy.integration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DispenseClinicalScreenRequestPayload {

    private UUID patientId;
    private String medicationCode;
    private String medicationName;
}
