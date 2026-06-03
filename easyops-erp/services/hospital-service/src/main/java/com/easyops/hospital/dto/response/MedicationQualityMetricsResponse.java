package com.easyops.hospital.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicationQualityMetricsResponse {
    private UUID patientId;
    private String patientName;
    private LocalDate reportDate;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal overallQualityScore; // 0-100
    private MedicationListQualityMetrics medicationListQuality;
    private ReconciliationComplianceMetrics reconciliationCompliance;
    private List<QualityIssue> qualityIssues;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MedicationListQualityMetrics {
        private BigDecimal dataQualityScore; // 0-100
        private Integer totalMedications;
        private Integer medicationsWithCompleteData;
        private Integer medicationsWithMissingData;
        private Map<String, Integer> dataCompletenessByField;
        private Integer duplicateMedications;
        private Integer medicationsWithConflictingData;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReconciliationComplianceMetrics {
        private BigDecimal complianceRate; // Percentage
        private Integer totalReconciliations;
        private Integer completedReconciliations;
        private Integer pendingReconciliations;
        private Integer overdueReconciliations;
        private LocalDate lastReconciliationDate;
        private Integer daysSinceLastReconciliation;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QualityIssue {
        private String issueType; // MISSING_DATA, DUPLICATE, CONFLICTING_DATA, INCOMPLETE_RECONCILIATION
        private String severity; // LOW, MEDIUM, HIGH, CRITICAL
        private String description;
        private UUID medicationId;
        private String medicationName;
        private String recommendation;
    }
}
