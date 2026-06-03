package com.easyops.hospital.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "pdmp_query_results", schema = "ehr")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class PDMPQueryResult {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "query_result_id")
    private UUID queryResultId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prescription_id", nullable = false)
    private Prescription prescription;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;
    
    // Query Information
    @Column(name = "query_date", nullable = false)
    private LocalDateTime queryDate;
    
    @Column(name = "query_state", length = 50, nullable = false)
    private String queryState; // State where PDMP query was performed
    
    @Column(name = "query_type", length = 50)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private QueryType queryType = QueryType.PATIENT_HISTORY;
    
    // Provider Information
    @Column(name = "querying_provider_id", nullable = false)
    private UUID queryingProviderId;
    
    @Column(name = "querying_provider_npi", length = 20)
    private String queryingProviderNpi;
    
    @Column(name = "querying_provider_name", length = 200)
    private String queryingProviderName;
    
    @Column(name = "dea_number", length = 20)
    private String deaNumber;
    
    // Query Status
    @Column(name = "query_status", length = 50, nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private QueryStatus queryStatus = QueryStatus.PENDING;
    
    @Column(name = "query_success")
    @Builder.Default
    private Boolean querySuccess = false;
    
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    
    // Query Results Summary
    @Column(name = "total_prescriptions")
    private Integer totalPrescriptions;
    
    @Column(name = "total_pharmacies")
    private Integer totalPharmacies;
    
    @Column(name = "total_prescribers")
    private Integer totalPrescribers;
    
    @Column(name = "date_range_start")
    private LocalDate dateRangeStart;
    
    @Column(name = "date_range_end")
    private LocalDate dateRangeEnd;
    
    @Column(name = "has_controlled_substances")
    @Builder.Default
    private Boolean hasControlledSubstances = false;
    
    @Column(name = "risk_score")
    private Integer riskScore; // 0-100 risk score if provided by PDMP
    
    @Column(name = "risk_level", length = 20)
    @Enumerated(EnumType.STRING)
    private RiskLevel riskLevel;
    
    // Detailed Results (stored as JSON)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "prescription_history", columnDefinition = "jsonb")
    private List<Map<String, Object>> prescriptionHistory;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "pharmacy_list", columnDefinition = "jsonb")
    private List<Map<String, Object>> pharmacyList;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "prescriber_list", columnDefinition = "jsonb")
    private List<Map<String, Object>> prescriberList;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_response", columnDefinition = "jsonb")
    private Map<String, Object> rawResponse; // Store full PDMP response
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "risk_indicators", columnDefinition = "jsonb")
    private Map<String, Object> riskIndicators;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "alerts", columnDefinition = "jsonb")
    private java.util.List<String> alerts;
    
    // Flags and Warnings
    @Column(name = "has_duplicate_prescriptions")
    @Builder.Default
    private Boolean hasDuplicatePrescriptions = false;
    
    @Column(name = "has_overlapping_prescriptions")
    @Builder.Default
    private Boolean hasOverlappingPrescriptions = false;
    
    @Column(name = "has_early_refills")
    @Builder.Default
    private Boolean hasEarlyRefills = false;
    
    @Column(name = "has_multiple_prescribers")
    @Builder.Default
    private Boolean hasMultiplePrescribers = false;
    
    @Column(name = "has_multiple_pharmacies")
    @Builder.Default
    private Boolean hasMultiplePharmacies = false;
    
    @Column(name = "warnings", columnDefinition = "TEXT")
    private String warnings; // Additional warnings or alerts
    
    // Documentation
    @Column(name = "query_reason", columnDefinition = "TEXT")
    private String queryReason; // Reason for query (e.g., "Prescribing Schedule II controlled substance")
    
    @Column(name = "clinical_notes", columnDefinition = "TEXT")
    private String clinicalNotes; // Provider's clinical notes about the query results
    
    @Column(name = "action_taken", columnDefinition = "TEXT")
    private String actionTaken; // What action was taken based on query results
    
    // External PDMP System Information
    @Column(name = "pdmp_system_name", length = 100)
    private String pdmpSystemName; // Name of the PDMP system queried
    
    @Column(name = "pdmp_system_id", length = 100)
    private String pdmpSystemId; // ID of the PDMP system
    
    @Column(name = "pdmp_query_id", length = 100)
    private String pdmpQueryId; // External query ID from PDMP system
    
    @Column(name = "pdmp_response_id", length = 100)
    private String pdmpResponseId; // External response ID from PDMP system
    
    // Audit Fields
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "created_by")
    private UUID createdBy;
    
    @Column(name = "updated_by")
    private UUID updatedBy;
    
    public enum QueryType {
        PATIENT_HISTORY,        // Query patient's prescription history
        PRESCRIPTION_CHECK,     // Check before prescribing
        REFILL_CHECK,          // Check before refill
        COMPLIANCE_CHECK,       // Compliance monitoring
        INVESTIGATION           // Investigation/audit
    }
    
    public enum QueryStatus {
        PENDING,               // Query submitted, waiting for response
        IN_PROGRESS,           // Query being processed
        COMPLETED,             // Query completed successfully
        FAILED,                // Query failed
        TIMEOUT,               // Query timed out
        CANCELLED              // Query was cancelled
    }
    
    public enum RiskLevel {
        LOW,                   // Low risk
        MODERATE,              // Moderate risk
        HIGH,                  // High risk
        CRITICAL,              // Critical risk
        UNKNOWN                // Unknown / not provided
    }
}
