package com.easyops.hospitalpharmacy.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "formulary_rules", schema = "hospital_pharmacy")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class FormularyRule {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "drug_id", nullable = false)
    private Drug drug;

    @Column(name = "restricted", nullable = false)
    @Builder.Default
    private boolean restricted = false;

    @Column(name = "restriction_reason", columnDefinition = "TEXT")
    private String restrictionReason;

    @Column(name = "ward_id")
    private UUID wardId;

    @Column(name = "department_id")
    private UUID departmentId;

    @Column(name = "corporate_contract_id")
    private UUID corporateContractId;

    @Column(name = "preferred_alternative_drug_ids", columnDefinition = "JSONB")
    private String preferredAlternativeDrugIdsJson;

    @Column(name = "effective_from")
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(name = "rule_set_version", length = 50)
    private String ruleSetVersion;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}

