package com.easyops.hr.entity;

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
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.EntityListeners;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * HR-AT-01: Organization shift master — type, grace, expected hours, optional OT band multiplier.
 */
@Entity
@Table(name = "shift_definitions", schema = "hr")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class ShiftDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "shift_definition_id")
    private UUID shiftDefinitionId;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    /** DAY, NIGHT, ROTATIONAL, … */
    @Column(name = "shift_type", nullable = false, length = 30)
    @Builder.Default
    private String shiftType = "DAY";

    @Column(name = "grace_minutes", nullable = false)
    @Builder.Default
    private Integer graceMinutes = 0;

    @Column(name = "expected_hours", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal expectedHours = new BigDecimal("8.00");

    /** HR-AT-05: when set, payroll OT_PAY uses this multiplier before org policy (after employee override). */
    @Column(name = "overtime_rate_multiplier", precision = 5, scale = 2)
    private BigDecimal overtimeRateMultiplier;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
