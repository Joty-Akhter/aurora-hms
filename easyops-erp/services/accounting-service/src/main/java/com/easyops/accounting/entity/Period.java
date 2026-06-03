package com.easyops.accounting.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "periods", schema = "accounting")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Period implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;
    
    @Column(name = "fiscal_year_id", nullable = false)
    private UUID fiscalYearId;
    
    @Column(name = "period_number", nullable = false)
    private Integer periodNumber;
    
    @Column(name = "period_name", nullable = false, length = 50)
    private String periodName;
    
    @Column(name = "period_type", length = 20)
    private String periodType = "MONTHLY"; // MONTHLY, QUARTERLY
    
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;
    
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;
    
    @Column(name = "status", length = 20)
    private String status = "OPEN"; // OPEN, CLOSED, LOCKED
    
    @Column(name = "closed_at")
    private LocalDateTime closedAt;
    
    @Column(name = "closed_by")
    private UUID closedBy;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}

