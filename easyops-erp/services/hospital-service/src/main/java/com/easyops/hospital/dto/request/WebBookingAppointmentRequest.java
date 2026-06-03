package com.easyops.hospital.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebBookingAppointmentRequest {

    @NotNull
    private UUID doctorId;

    @NotBlank
    private String fullName;

    @NotBlank
    private String primaryPhone;

    private String primaryEmail;

  /** Approximate age when date of birth is not supplied. */
    private Integer ageYears;

    @NotBlank
    private String appointmentDate;

    @NotBlank
    private String slotStart;

    @NotBlank
    private String slotEnd;
}
