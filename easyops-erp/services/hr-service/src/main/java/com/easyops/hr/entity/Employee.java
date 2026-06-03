package com.easyops.hr.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "employees", schema = "hr")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Employee {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "employee_id")
    private UUID employeeId;
    
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;
    
    @Column(name = "user_id")
    private UUID userId;
    
    @Column(name = "employee_number", nullable = false, length = 50)
    private String employeeNumber;
    
    @Column(name = "name", nullable = false, length = 200)
    private String name;
    
    @Column(name = "email", length = 255)
    private String email;
    
    @Column(name = "phone", length = 50)
    private String phone;
    
    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;
    
    @Column(name = "gender", length = 20)
    private String gender;
    
    @Column(name = "hire_date", nullable = false)
    private LocalDate hireDate;
    
    @Column(name = "termination_date")
    private LocalDate terminationDate;
    
    @Column(name = "department_id")
    private UUID departmentId;
    
    @Column(name = "position_id")
    private UUID positionId;
    
    @Column(name = "manager_id")
    private UUID managerId;
    
    @Column(name = "employment_type", length = 50)
    @Builder.Default
    private String employmentType = "FULL_TIME";
    
    @Column(name = "employment_status", length = 50)
    @Builder.Default
    private String employmentStatus = "ACTIVE";
    
    // Address information
    @Column(name = "address_line1", length = 255)
    private String addressLine1;
    
    @Column(name = "address_line2", length = 255)
    private String addressLine2;
    
    @Column(name = "city", length = 100)
    private String city;
    
    @Column(name = "state_province", length = 100)
    private String stateProvince;
    
    @Column(name = "postal_code", length = 20)
    private String postalCode;
    
    @Column(name = "country", length = 100)
    private String country;
    
    // Emergency contact
    @Column(name = "emergency_contact_name", length = 200)
    private String emergencyContactName;
    
    @Column(name = "emergency_contact_phone", length = 50)
    private String emergencyContactPhone;
    
    @Column(name = "emergency_contact_relationship", length = 100)
    private String emergencyContactRelationship;

    // HR-MD-04 bank details (encrypt-at-rest is deployment-specific)
    @Column(name = "bank_name", length = 200)
    private String bankName;

    @Column(name = "bank_branch", length = 200)
    private String bankBranch;

    @Column(name = "bank_account_number", length = 100)
    private String bankAccountNumber;

    @Column(name = "bank_routing_or_iban", length = 64)
    private String bankRoutingOrIban;

    /** HR-AT-05: optional OT multiplier override (employee category precedence). */
    @Column(name = "payroll_overtime_rate_multiplier", precision = 5, scale = 2)
    private BigDecimal payrollOvertimeRateMultiplier;

    /** HR-AT-05: optional standard hours/day for OT hourly rate derivation. */
    @Column(name = "payroll_standard_hours_per_day", precision = 5, scale = 2)
    private BigDecimal payrollStandardHoursPerDay;
    
    // System fields
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;
    
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @Column(name = "created_by", length = 100)
    private String createdBy;
    
    @Column(name = "updated_by", length = 100)
    private String updatedBy;
    
    // Helper method
    public String getFullName() {
        return name != null ? name : "";
    }
}

