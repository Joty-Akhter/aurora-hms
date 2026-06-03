package com.easyops.hospitalscheduling.api.dto;

import jakarta.validation.constraints.Size;

import java.util.UUID;

public class UpdateResourceRequest {

    @Size(max = 30)
    private String resourceType;

    @Size(max = 255)
    private String externalReferenceId;

    @Size(max = 255)
    private String name;

    private UUID branchId;
    private UUID departmentId;
    private String metadata;
    @Size(max = 20)
    private String status;

    public String getResourceType() { return resourceType; }
    public void setResourceType(String resourceType) { this.resourceType = resourceType; }
    public String getExternalReferenceId() { return externalReferenceId; }
    public void setExternalReferenceId(String externalReferenceId) { this.externalReferenceId = externalReferenceId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public UUID getBranchId() { return branchId; }
    public void setBranchId(UUID branchId) { this.branchId = branchId; }
    public UUID getDepartmentId() { return departmentId; }
    public void setDepartmentId(UUID departmentId) { this.departmentId = departmentId; }
    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
