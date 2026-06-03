package com.easyops.hr.dto.roster;

import com.easyops.hr.entity.Holiday;
import com.easyops.hr.entity.LeaveRequest;

import java.util.List;

public record RosterMonthViewDto(
        List<RosterScheduleRowDto> schedules,
        List<Holiday> holidays,
        List<LeaveRequest> approvedLeaves,
        List<RosterConflictWarningDto> conflictWarnings
) {}
