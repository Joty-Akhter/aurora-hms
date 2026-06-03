package com.easyops.hr.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayrollTimeAttendancePolicyDto {
    private UUID organizationId;
    private BigDecimal overtimeRateMultiplier;
    private Boolean inferMissingWeekdayLop;
    private BigDecimal standardHoursPerDay;
    /** HR-LV-03 */
    private Boolean leavePayrollBridgeEnabled;
    private Boolean unpaidApprovedLeaveCountsAsLop;
    /** HR-LV-02 */
    private Boolean excludeActiveHolidaysFromWorkingDays;
    private Boolean excludeActiveHolidaysFromLopInference;
}
