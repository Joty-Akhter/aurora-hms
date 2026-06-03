package com.easyops.hr.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/** HR-AT-03: Result of a device punch ingest or a raw-log processing run. */
@Data
@Builder
public class DeviceSyncResultDto {

    /** Number of punch records saved to attendance_raw_logs. */
    private int ingested;

    /** Number of attendance_records created or updated during processing. */
    private int attendanceRecordsAffected;

    /** Punches skipped because employeeId could not be resolved. */
    private int skipped;

    /** Non-fatal warnings or resolution failures. */
    private List<String> warnings;
}
