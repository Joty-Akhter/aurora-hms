package com.easyops.hospital.dto.response;

import com.easyops.hospital.dto.DoctorAppointmentSlot;
import com.easyops.hospital.dto.DoctorWeeklySchedule;
import com.easyops.hospital.entity.Doctor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoctorResponse {
    
    private UUID doctorId;
    private String doctorCode;
    private String doctorName;
    
    // Department Information
    private UUID departmentId;
    private String departmentName;
    
    private Doctor.DoctorType doctorType;
    private Doctor.IndoorOutdoorStatus indoorOutdoorStatus;
    private String degree;
    private String speciality;
    private String gender;
    private LocalDate birthDate;
    private LocalDate registrationDate;
    private String bmdcRegistrationNumber;
    
    // Contact Information
    private String phoneNumber;
    private String email;
    private String presentAddress;
    private String district;
    private String thana;
    private String area;
    private String chamberRoom;
    
    // Financial Information
    private BigDecimal visitFeeNew;
    private BigDecimal visitFeeOld;
    private Boolean takeCommission;
    
    // Appointment Settings
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
    
    // Status Information
    private Doctor.PrescriptionStatus prescriptionStatus;
    private Doctor.AvailabilityStatus availabilityStatus;
    private Boolean isActive;

    /** users.users.id when a portal account exists for this doctor */
    private UUID linkedUserId;
    
    // Audit Fields
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
}
