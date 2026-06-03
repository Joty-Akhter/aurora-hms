package com.easyops.hospital.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "social_history", schema = "ehr")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class SocialHistory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "social_history_id")
    private UUID socialHistoryId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;
    
    @Column(name = "category", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private Category category;
    
    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private Status status;
    
    @Column(name = "frequency", length = 100)
    private String frequency;
    
    @Column(name = "quantity", length = 100)
    private String quantity;
    
    @Column(name = "duration_years")
    private Integer durationYears;
    
    @Column(name = "start_date")
    private LocalDate startDate;
    
    @Column(name = "end_date")
    private LocalDate endDate;
    
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
    
    @Column(name = "documented_date")
    @Builder.Default
    private LocalDate documentedDate = LocalDate.now();
    
    @Column(name = "documented_by")
    private UUID documentedBy;
    
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
    
    public enum Category {
        SMOKING, ALCOHOL, DRUGS, OCCUPATION, LIFESTYLE, EXERCISE, DIET, OTHER
    }
    
    public enum Status {
        CURRENT, PAST, NEVER
    }
}
