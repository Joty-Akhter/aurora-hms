package com.easyops.hr.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * SS-28: Audit log for salary structure, grade, and band changes.
 * Records user, timestamp, entity, action, and old/new values.
 */
@Entity
@Table(name = "salary_audit_log", schema = "hr")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SalaryAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "audit_id")
    private UUID auditId;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "entity_type", nullable = false, length = 30)
    private String entityType;

    @Column(name = "entity_id", nullable = false)
    private UUID entityId;

    @Column(name = "action", nullable = false, length = 20)
    private String action;

    @Column(name = "performed_by", length = 100)
    private String performedBy;

    @Column(name = "performed_at", nullable = false)
    private LocalDateTime performedAt;

    @Column(name = "old_values", columnDefinition = "TEXT")
    private String oldValues;

    @Column(name = "new_values", columnDefinition = "TEXT")
    private String newValues;

    public static final String ENTITY_STRUCTURE = "STRUCTURE";
    public static final String ENTITY_GRADE = "GRADE";
    public static final String ENTITY_BAND = "BAND";
    /** SC-30: Audit log for salary component master changes. */
    public static final String ENTITY_COMPONENT = "COMPONENT";
    /** ES-36: Audit log for employee salary detail (component value) changes. */
    public static final String ENTITY_EMPLOYEE_SALARY_DETAIL = "EMPLOYEE_SALARY_DETAIL";
    /** ES-36: Audit log for employee salary assignment changes. */
    public static final String ENTITY_EMPLOYEE_SALARY_ASSIGNMENT = "EMPLOYEE_SALARY_ASSIGNMENT";
    public static final String ACTION_CREATE = "CREATE";
    public static final String ACTION_UPDATE = "UPDATE";
    public static final String ACTION_DEACTIVATE = "DEACTIVATE";
}
