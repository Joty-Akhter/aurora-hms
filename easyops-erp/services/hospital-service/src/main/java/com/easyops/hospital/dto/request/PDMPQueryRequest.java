package com.easyops.hospital.dto.request;

import com.easyops.hospital.entity.PDMPQueryResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PDMPQueryRequest {
    
    @NotNull(message = "Patient ID is required")
    private UUID patientId;
    
    @NotNull(message = "Prescription ID is required")
    private UUID prescriptionId;
    
    @NotNull(message = "Query state is required")
    private String queryState; // State where PDMP query should be performed
    
    private PDMPQueryResult.QueryType queryType;
    
    private String queryReason; // Reason for query
    
    private String deaNumber; // DEA number of prescribing provider
    
    // Optional: Override patient information if needed
    private String patientFirstName;
    private String patientLastName;
    private String patientDateOfBirth; // Format: YYYY-MM-DD
    private String patientIdNo; // Last 4 digits or full ID number
    private String patientState; // Patient's state of residence
    
    // Optional: Date range for query
    private String dateRangeStart; // Format: YYYY-MM-DD
    private String dateRangeEnd; // Format: YYYY-MM-DD
}
