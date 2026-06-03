package com.easyops.hospital.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DoctorAppointmentSlot {
    /** Start time in HH:mm format, e.g. "09:00" */
    private String startTime;
    /** End time in HH:mm format, e.g. "17:00" */
    private String endTime;
    /** Days this slot applies to, e.g. ["saturday", "sunday", "monday"] */
    private List<String> days;
    /** Maximum number of appointments in this slot */
    private Integer maxPatients;
}
