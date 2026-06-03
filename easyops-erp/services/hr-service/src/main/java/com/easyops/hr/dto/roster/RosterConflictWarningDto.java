package com.easyops.hr.dto.roster;

import java.time.LocalDate;
import java.util.UUID;

public record RosterConflictWarningDto(
        String warningType,
        UUID employeeId,
        String employeeName,
        LocalDate date,
        String message
) {}
