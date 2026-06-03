package com.easyops.hospital.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DoctorDaySchedule {
    /** True when the doctor is off for the entire day */
    private boolean off;
    /** Appointment slots for the day; empty when off=true */
    private List<DoctorAppointmentSlot> slots;
}
