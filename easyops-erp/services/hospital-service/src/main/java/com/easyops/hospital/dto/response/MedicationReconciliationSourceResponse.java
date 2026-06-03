package com.easyops.hospital.dto.response;

import com.easyops.hospital.entity.MedicationReconciliationSource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicationReconciliationSourceResponse {
    
    private UUID sourceId;
    private UUID reconciliationId;
    private MedicationReconciliationSource.SourceType sourceType;
    private String sourceName;
    private String sourceDescription;
    private Map<String, Object> sourceData;
    private LocalDate sourceDate;
    private String sourceProviderName;
    private String sourceFacilityName;
    private String sourceContactInfo;
    private LocalDateTime importedAt;
    private UUID importedBy;
    private MedicationReconciliationSource.ImportMethod importMethod;
    private LocalDateTime createdAt;
    private UUID createdBy;
}
