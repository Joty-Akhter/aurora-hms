package com.easyops.hospital.dto.request;

import com.easyops.hospital.entity.DoctorDepartment;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoctorDepartmentRequest {
    
    @NotBlank(message = "Department name is required")
    private String departmentName;
    
    private BigDecimal generalVisitAmount;
    
    private DoctorDepartment.DepartmentStatus status;
}
