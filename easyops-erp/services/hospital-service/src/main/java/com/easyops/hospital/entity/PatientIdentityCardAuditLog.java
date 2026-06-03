package com.easyops.hospital.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "patient_identity_card_audit_log", schema = "ehr")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientIdentityCardAuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "audit_id")
    private UUID auditId;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "card_id")
    private UUID cardId;

    @Column(name = "card_number", length = 100)
    private String cardNumber;

    @Column(name = "action", nullable = false, length = 20)
    private String action;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "printed_by")
    private UUID printedBy;

    @Column(name = "printed_at", nullable = false)
    private OffsetDateTime printedAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
}
