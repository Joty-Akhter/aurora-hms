package com.easyops.hospital.dto.request;

import com.easyops.hospital.dto.DoctorAppointmentSlot;
import com.easyops.hospital.dto.DoctorWeeklySchedule;
import com.easyops.hospital.entity.Doctor;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoctorRequest {
    
    @NotBlank(message = "Doctor name is required")
    private String doctorName;
    
    @NotNull(message = "Department ID is required")
    private UUID departmentId;
    
    @NotNull(message = "Doctor type is required")
    private Doctor.DoctorType doctorType;
    
    @NotNull(message = "Indoor/Outdoor status is required")
    private Doctor.IndoorOutdoorStatus indoorOutdoorStatus;
    
    @NotBlank(message = "Degree is required")
    private String degree;

    private String speciality;
    private String gender;
    private LocalDate birthDate;
    
    @NotNull(message = "Registration date is required")
    private LocalDate registrationDate;
    
    @NotBlank(message = "BMDC registration number is required")
    private String bmdcRegistrationNumber;
    private String phoneNumber;
    
    @Email(message = "Invalid email format")
    private String email;
    
    private String presentAddress;
    private String district;
    private String thana;
    private String area;
    private String chamberRoom;
    
    private BigDecimal visitFeeNew;
    private BigDecimal visitFeeOld;
    private Boolean takeCommission;
    
    private Integer patientsPerDay;
    private Integer serialStartFrom;
    private Integer numberOfDaysCanAppointment;
    private Integer numberOfAppointmentsFromWeb;
    private Integer numberOfAppointmentsFromMobile;
    private Boolean appointmentsFromWeb;
    private Boolean appointmentsFromMobile;
    private Integer slotsPerDay;
    private DoctorWeeklySchedule weeklySchedule;
    private List<DoctorAppointmentSlot> appointmentSlots;
    private List<String> offDays;
    private Boolean smsEnabled;
    
    private Doctor.PrescriptionStatus prescriptionStatus;
    private Doctor.AvailabilityStatus availabilityStatus;
    private Boolean isActive;

    /**
     * Portal login (username = doctor code; password from server config): created by default on create when the
     * doctor is active; set {@code false} to skip (e.g. bulk imports). On update, set {@code true} only when the
     * doctor has no {@code linked_user_id} yet to provision a login for legacy rows (doctor must be active).
     */
    private Boolean createLinkedUser;
}
