package com.easyops.hr.dto.roster;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record RosterScheduleRowDto(
        UUID scheduleId,
        UUID employeeId,
        String employeeName,
        LocalDate shiftDate,
        UUID shiftDefinitionId,
        String shiftDefinitionName,
        String shiftName,
        LocalTime startTime,
        LocalTime endTime,
        Integer breakDuration,
        Boolean isOvertime,
        String notes
) {}
