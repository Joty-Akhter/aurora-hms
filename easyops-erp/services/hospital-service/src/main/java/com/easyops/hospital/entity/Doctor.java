package com.easyops.hospital.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.easyops.hospital.dto.DoctorAppointmentSlot;
import com.easyops.hospital.dto.DoctorWeeklySchedule;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "doctors", schema = "hospital")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Doctor {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "doctor_id")
    private UUID doctorId;
    
    @Column(name = "doctor_code", nullable = false, unique = true, length = 50)
    private String doctorCode;
    
    @Column(name = "doctor_name", nullable = false, length = 100)
    private String doctorName;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false)
    private DoctorDepartment department;
    
    @Column(name = "doctor_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private DoctorType doctorType;
    
    @Column(name = "indoor_outdoor_status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private IndoorOutdoorStatus indoorOutdoorStatus;
    
    @Column(name = "degree", length = 200)
    private String degree;
    
    @Column(name = "speciality", length = 200)
    private String speciality;
    
    @Column(name = "gender", length = 20)
    private String gender;
    
    @Column(name = "birth_date")
    private LocalDate birthDate;
    
    @Column(name = "registration_date", nullable = false)
    private LocalDate registrationDate;
    
    @Column(name = "bmdc_registration_number", length = 50)
    private String bmdcRegistrationNumber;
    
    @Column(name = "phone_number", length = 20)
    private String phoneNumber;
    
    @Column(name = "email", unique = true, length = 100)
    private String email;
    
    @Column(name = "present_address", columnDefinition = "TEXT")
    private String presentAddress;
    
    @Column(name = "district", length = 100)
    private String district;
    
    @Column(name = "thana", length = 100)
    private String thana;
    
    @Column(name = "area", length = 100)
    private String area;
    
    @Column(name = "chamber_room", length = 100)
    private String chamberRoom;
    
    @Column(name = "visit_fee_new", precision = 10, scale = 2)
    private BigDecimal visitFeeNew;
    
    @Column(name = "visit_fee_old", precision = 10, scale = 2)
    private BigDecimal visitFeeOld;
    
    @Column(name = "take_commission")
    @Builder.Default
    private Boolean takeCommission = false;
    
    @Column(name = "patients_per_day")
    private Integer patientsPerDay;
    
    @Column(name = "serial_start_from")
    @Builder.Default
    private Integer serialStartFrom = 1;
    
    @Column(name = "number_of_days_can_appointment")
    private Integer numberOfDaysCanAppointment;
    
    @Column(name = "number_of_appointments_from_web")
    private Integer numberOfAppointmentsFromWeb;

    @Column(name = "number_of_appointments_from_mobile")
    private Integer numberOfAppointmentsFromMobile;

    @Column(name = "appointments_from_web")
    @Builder.Default
    private Boolean appointmentsFromWeb = false;

    @Column(name = "appointments_from_mobile")
    @Builder.Default
    private Boolean appointmentsFromMobile = false;

    @Column(name = "slots_per_day")
    @Builder.Default
    private Integer slotsPerDay = 1;

    @Column(name = "weekly_schedule", columnDefinition = "TEXT")
    @Convert(converter = WeeklyScheduleConverter.class)
    private DoctorWeeklySchedule weeklySchedule;

    @Column(name = "appointment_slots", columnDefinition = "TEXT")
    @Convert(converter = AppointmentSlotsConverter.class)
    private List<DoctorAppointmentSlot> appointmentSlots;

    @Column(name = "off_days", columnDefinition = "TEXT")
    @Convert(converter = StringListConverter.class)
    private List<String> offDays;

    @Column(name = "sms_enabled")
    @Builder.Default
    private Boolean smsEnabled = false;
    
    @Column(name = "prescription_status", length = 50)
    @Enumerated(EnumType.STRING)
    private PrescriptionStatus prescriptionStatus;
    
    @Column(name = "availability_status", length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private AvailabilityStatus availabilityStatus = AvailabilityStatus.AVAILABLE;
    
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    /** {@code users.users.id} when a portal user was created for this doctor */
    @Column(name = "linked_user_id")
    private UUID linkedUserId;
    
    // Audit Fields
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "created_by", length = 100)
    private String createdBy;
    
    @Column(name = "updated_by", length = 100)
    private String updatedBy;
    
    public enum DoctorType {
        CONSULTANT,
        RESIDENT,
        INTERN,
        SENIOR_CONSULTANT,
        ASSOCIATE_CONSULTANT,
        ASSISTANT_CONSULTANT,
        REGISTRAR,
        MEDICAL_OFFICER,
        OTHER
    }
    
    public enum IndoorOutdoorStatus {
        INDOOR,
        OUTDOOR
    }
    
    public enum PrescriptionStatus {
        ACTIVE,
        INACTIVE
    }
    
    public enum AvailabilityStatus {
        AVAILABLE,
        NOT_AVAILABLE
    }
}
