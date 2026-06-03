package com.easyops.hospital.dto.response;

import com.easyops.hospital.entity.DoctorDepartment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoctorDepartmentResponse {
    
    private UUID departmentId;
    private String departmentName;
    private BigDecimal generalVisitAmount;
    private DoctorDepartment.DepartmentStatus status;
    
    // Audit Fields
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
}
