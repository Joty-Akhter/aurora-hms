package com.easyops.organization.dto;

import com.easyops.organization.entity.Department;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Department Response DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentResponse {

    private UUID id;
    private UUID organizationId;
    private UUID parentDepartmentId;
    private String code;
    private String name;
    private String description;
    private String type;
    private UUID managerId;
    private String costCenter;
    private String status;
    private ZonedDateTime createdAt;
    private ZonedDateTime updatedAt;
    private List<DepartmentResponse> children; // For hierarchical tree

    public static DepartmentResponse fromEntity(Department dept) {
        return DepartmentResponse.builder()
                .id(dept.getId())
                .organizationId(dept.getOrganizationId())
                .parentDepartmentId(dept.getParentDepartmentId())
                .code(dept.getCode())
                .name(dept.getName())
                .description(dept.getDescription())
                .type(dept.getType())
                .managerId(dept.getManagerId())
                .costCenter(dept.getCostCenter())
                .status(dept.getStatus())
                .createdAt(dept.getCreatedAt())
                .updatedAt(dept.getUpdatedAt())
                .build();
    }
}

