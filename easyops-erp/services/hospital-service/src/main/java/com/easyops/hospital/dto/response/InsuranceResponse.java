package com.easyops.hospital.dto.response;

import com.easyops.hospital.entity.PatientInsurance;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InsuranceResponse {
    
    private UUID insuranceId;
    private UUID patientId;
    private PatientInsurance.InsuranceType insuranceType;
    private String insuranceCompanyName;
    private String policyNumber;
    private String groupNumber;
    private String subscriberName;
    private LocalDate subscriberDob;
    private PatientInsurance.SubscriberRelationship subscriberRelationship;
    private LocalDate effectiveDate;
    private LocalDate expirationDate;
    private BigDecimal copayAmount;
    private PatientInsurance.VerificationStatus verificationStatus;
    private LocalDate verifiedDate;
    private String insurancePhone;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
