package com.easyops.hr.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SC-41, SC-32, SC-34: Usage of a salary component — employee count, formula references, and payroll result count.
 * Used to warn on deactivate (SC-34) and to prevent delete when in use (SC-32).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComponentUsageDto {
    private long employeeCount;
    private long referencedInFormulasCount;
    /** SC-32: Count of payroll result lines referencing this component (past payroll). */
    private long payrollResultCount;
}
