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
public class WebBookingAppointmentResponse {
    private UUID appointmentId;
    private UUID patientId;
    private String mrn;
    private String message;
}
