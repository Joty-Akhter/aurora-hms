package com.easyops.hospital.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class PatientIdentityCardActionResponse {
    private UUID patientId;
    private UUID cardId;
    private String cardNumber;
    private String status;
    private String message;
    private String action; // REPLACE
    private String reason;
    private UUID performedBy;
    private OffsetDateTime performedAt;
}
