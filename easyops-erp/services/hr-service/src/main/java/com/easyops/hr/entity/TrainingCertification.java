package com.easyops.hr.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "training_certifications", schema = "hr")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class TrainingCertification {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "training_id")
    private UUID trainingId;
    
    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;
    
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;
    
    @Column(name = "training_name", nullable = false, length = 200)
    private String trainingName;
    
    @Column(name = "training_type", length = 50)
    private String trainingType;
    
    @Column(name = "provider", length = 200)
    private String provider;
    
    @Column(name = "start_date")
    private LocalDate startDate;
    
    @Column(name = "completion_date")
    private LocalDate completionDate;
    
    @Column(name = "expiry_date")
    private LocalDate expiryDate;
    
    @Column(name = "certification_number", length = 100)
    private String certificationNumber;
    
    @Column(name = "status", length = 50)
    private String status = "planned";
    
    @Column(name = "cost", precision = 10, scale = 2)
    private BigDecimal cost;
    
    @Column(name = "currency", length = 3)
    private String currency = "BDT";
    
    @Column(name = "hours_spent", precision = 6, scale = 2)
    private BigDecimal hoursSpent;
    
    @Column(name = "score", precision = 5, scale = 2)
    private BigDecimal score;
    
    @Column(name = "certificate_url", length = 500)
    private String certificateUrl;
    
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
    
    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private String createdBy;
    
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedBy
    @Column(name = "updated_by")
    private String updatedBy;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

