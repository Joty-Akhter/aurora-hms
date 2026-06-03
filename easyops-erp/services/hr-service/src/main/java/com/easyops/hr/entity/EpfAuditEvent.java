package com.easyops.hr.entity;

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
@Table(name = "epf_audit_events", schema = "hr")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class EpfAuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "event_id")
    private UUID eventId;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "epf_account_id")
    private UUID epfAccountId;

    @Column(name = "employee_id")
    private UUID employeeId;

    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType; // account, contribution, withdrawal, transfer, filing, remittance, compliance

    @Column(name = "entity_id")
    private UUID entityId;

    @Column(name = "event_type", nullable = false, length = 80)
    private String eventType; // contribution_created, withdrawal_approved, etc.

    @Column(name = "actor_user_id", length = 100)
    private String actorUserId;

    @Column(name = "event_data", columnDefinition = "TEXT")
    private String eventData;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
