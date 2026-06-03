package com.easyops.hr.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/** HR-AT-03: One punch event from a biometric / access-control device. */
@Data
public class DevicePunchDto {

    /** Resolved employee UUID (preferred). May be null when device sends rawEmployeeCode only. */
    private UUID employeeId;

    /** Device identifier (name or serial). */
    private String deviceId;

    /** Exact punch timestamp from the device. */
    private LocalDateTime punchTime;

    /** IN | OUT | UNKNOWN. Null is treated as UNKNOWN. */
    private String punchType;

    /** Device-side employee code, used to resolve employeeId when UUID is not available. */
    private String rawEmployeeCode;

    /** Optional notes (e.g. device event code). */
    private String notes;
}
