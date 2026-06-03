package com.easyops.hr.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "epf_correction_logs", schema = "hr")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class EpfCorrectionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "correction_id")
    private UUID correctionId;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "epf_account_id")
    private UUID epfAccountId;

    @Column(name = "entity_type", nullable = false, length = 40)
    private String entityType; // contribution, withdrawal, transfer

    @Column(name = "entity_id", nullable = false)
    private UUID entityId;

    @Column(name = "action_type", nullable = false, length = 40)
    private String actionType; // reversal, adjustment

    @Column(name = "amount_impact", precision = 15, scale = 2)
    private BigDecimal amountImpact;

    @Column(name = "reason", nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "reversed_by", length = 100)
    private String reversedBy;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
