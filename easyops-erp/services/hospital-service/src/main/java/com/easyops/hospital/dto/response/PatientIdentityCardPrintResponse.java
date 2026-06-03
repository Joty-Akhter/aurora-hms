package com.easyops.hospital.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class PatientIdentityCardPrintResponse {
    private UUID patientId;
    private String mrn;
    private UUID cardId;
    private String cardNumber;
    private String title;
    private String html;
    private String action; // PRINT or REPRINT
    private UUID printedBy;
    private OffsetDateTime printedAt;
}
