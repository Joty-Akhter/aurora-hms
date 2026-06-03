package com.easyops.hospital.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Weekly appointment schedule for a doctor.
 * Week starts on Saturday and ends on Friday.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DoctorWeeklySchedule {
    private DoctorDaySchedule saturday;
    private DoctorDaySchedule sunday;
    private DoctorDaySchedule monday;
    private DoctorDaySchedule tuesday;
    private DoctorDaySchedule wednesday;
    private DoctorDaySchedule thursday;
    private DoctorDaySchedule friday;
}
