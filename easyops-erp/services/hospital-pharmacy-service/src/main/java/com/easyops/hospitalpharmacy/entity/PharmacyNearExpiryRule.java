package com.easyops.hospitalpharmacy.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "pharmacy_near_expiry_rules", schema = "hospital_pharmacy")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class PharmacyNearExpiryRule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "therapeutic_class_id")
    private UUID therapeuticClassId;

    @Column(name = "days_before_expiry", nullable = false)
    private int daysBeforeExpiry;

    @Column(name = "action", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private Action action;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(name = "discount_required", nullable = false)
    @Builder.Default
    private boolean discountRequired = false;

    @Column(name = "approver_role_code", length = 100)
    private String approverRoleCode;

    public enum Action {
        BLOCK,
        WARN,
        ALLOW,
        ALLOW_WITH_APPROVAL
    }
}
