package com.easyops.hospital.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebBookableDoctorResponse {
    private UUID doctorId;
    private String doctorName;
    private String doctorCode;
    private String speciality;
    private String departmentName;
    private Integer numberOfDaysCanAppointment;
}
