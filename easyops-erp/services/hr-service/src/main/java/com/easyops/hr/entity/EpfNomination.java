package com.easyops.hr.entity;

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
@Table(name = "epf_nominations", schema = "hr")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class EpfNomination {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "nomination_id")
    private UUID nominationId;
    
    @Column(name = "epf_account_id", nullable = false)
    private UUID epfAccountId;
    
    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;
    
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;
    
    @Column(name = "nominee_name", nullable = false, length = 200)
    private String nomineeName;
    
    @Column(name = "nominee_relationship", nullable = false, length = 100)
    private String nomineeRelationship;
    
    @Column(name = "nominee_date_of_birth")
    private LocalDate nomineeDateOfBirth;
    
    @Column(name = "nominee_address", columnDefinition = "TEXT")
    private String nomineeAddress;
    
    @Column(name = "nominee_phone", length = 50)
    private String nomineePhone;
    
    @Column(name = "nominee_email", length = 255)
    private String nomineeEmail;
    
    @Column(name = "share_percentage", nullable = false, precision = 5, scale = 2)
    private java.math.BigDecimal sharePercentage;
    
    @Column(name = "is_primary")
    @Builder.Default
    private Boolean isPrimary = false;
    
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;
    
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @Column(name = "created_by", length = 100)
    private String createdBy;
    
    @Column(name = "updated_by", length = 100)
    private String updatedBy;
}

