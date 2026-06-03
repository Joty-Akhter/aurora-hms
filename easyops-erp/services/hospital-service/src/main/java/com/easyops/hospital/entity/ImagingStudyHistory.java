package com.easyops.hospital.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "imaging_study_history", schema = "ehr")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class ImagingStudyHistory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "history_id")
    private UUID historyId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "study_id", nullable = false)
    private ImagingStudy study;
    
    @Column(name = "change_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private ChangeType changeType;
    
    @Column(name = "changed_by", nullable = false)
    private UUID changedBy;
    
    @CreatedDate
    @Column(name = "changed_date", nullable = false, updatable = false)
    private LocalDateTime changedDate;
    
    @Column(name = "field_name", length = 100)
    private String fieldName;
    
    @Column(name = "previous_value", columnDefinition = "TEXT")
    private String previousValue;
    
    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;
    
    @Column(name = "change_reason", columnDefinition = "TEXT")
    private String changeReason;
    
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
    
    public enum ChangeType {
        CREATED, UPDATED, CORRECTED, AMENDED, ADDENDUM, CANCELLED, REVIEWED, ACKNOWLEDGED
    }
}
