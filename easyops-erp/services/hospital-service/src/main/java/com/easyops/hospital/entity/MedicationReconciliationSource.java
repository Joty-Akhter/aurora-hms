package com.easyops.hospital.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "medication_reconciliation_sources", schema = "ehr")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class MedicationReconciliationSource {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "source_id")
    private UUID sourceId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reconciliation_id", nullable = false)
    private MedicationReconciliation reconciliation;
    
    // Source Information
    @Column(name = "source_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private SourceType sourceType;
    
    @Column(name = "source_name", length = 200)
    private String sourceName;
    
    @Column(name = "source_description", columnDefinition = "TEXT")
    private String sourceDescription;
    
    // Source Data (stored as JSON)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "source_data", columnDefinition = "jsonb")
    private Map<String, Object> sourceData;
    
    // Source Metadata
    @Column(name = "source_date")
    private LocalDate sourceDate;
    
    @Column(name = "source_provider_name", length = 200)
    private String sourceProviderName;
    
    @Column(name = "source_facility_name", length = 200)
    private String sourceFacilityName;
    
    @Column(name = "source_contact_info", columnDefinition = "TEXT")
    private String sourceContactInfo;
    
    // Import Information
    @Column(name = "imported_at")
    private LocalDateTime importedAt;
    
    @Column(name = "imported_by")
    private UUID importedBy;
    
    @Column(name = "import_method", length = 50)
    @Enumerated(EnumType.STRING)
    private ImportMethod importMethod;
    
    // Audit Fields
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "created_by")
    private UUID createdBy;
    
    public enum SourceType {
        EHR_CURRENT, EHR_PREVIOUS, PATIENT_REPORTED, PHARMACY, 
        DISCHARGE_LIST, EXTERNAL_PROVIDER, OTHER_EHR
    }
    
    public enum ImportMethod {
        MANUAL, API, FILE_UPLOAD, HL7, FHIR
    }
}
