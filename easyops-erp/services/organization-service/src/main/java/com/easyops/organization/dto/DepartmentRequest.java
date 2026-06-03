package com.easyops.organization.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Department Request DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentRequest {

    private UUID parentDepartmentId;

    @NotBlank(message = "Department code is required")
    @Size(max = 50)
    private String code;

    @NotBlank(message = "Department name is required")
    @Size(max = 255)
    private String name;

    private String description;

    private String type;

    private UUID managerId;

    private String costCenter;

    private String status;
}

