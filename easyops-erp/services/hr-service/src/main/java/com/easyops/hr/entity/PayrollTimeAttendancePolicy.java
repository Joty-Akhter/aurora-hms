package com.easyops.hr.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Organization-level rules for payroll time &amp; attendance roll-up (OT rate, missing-day LOP, standard hours).
 */
@Entity
@Table(name = "payroll_time_attendance_policy", schema = "hr")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayrollTimeAttendancePolicy {

    @Id
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "overtime_rate_multiplier", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal overtimeRateMultiplier = new BigDecimal("1.50");

    @Column(name = "infer_missing_weekday_lop", nullable = false)
    @Builder.Default
    private Boolean inferMissingWeekdayLop = Boolean.TRUE;

    @Column(name = "standard_hours_per_day", nullable = false, precision = 4, scale = 2)
    @Builder.Default
    private BigDecimal standardHoursPerDay = new BigDecimal("8.00");

    /** HR-LV-03: use approved paid leave to suppress inferred LOP on weekdays without attendance rows. */
    @Column(name = "leave_payroll_bridge_enabled", nullable = false)
    @Builder.Default
    private Boolean leavePayrollBridgeEnabled = Boolean.FALSE;

    /** When bridge is on and infer_missing_weekday_lop is on: unpaid approved leave adds inferred LOP for uncovered weekdays. */
    @Column(name = "unpaid_approved_leave_counts_as_lop", nullable = false)
    @Builder.Default
    private Boolean unpaidApprovedLeaveCountsAsLop = Boolean.TRUE;

    /** Subtract applicable holidays (weekdays) from working-day count used for OT/LOP divisors. */
    @Column(name = "exclude_active_holidays_from_working_days", nullable = false)
    @Builder.Default
    private Boolean excludeActiveHolidaysFromWorkingDays = Boolean.FALSE;

    /** Skip inferred LOP on weekdays that fall on applicable holidays. */
    @Column(name = "exclude_active_holidays_from_lop_inference", nullable = false)
    @Builder.Default
    private Boolean excludeActiveHolidaysFromLopInference = Boolean.FALSE;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
