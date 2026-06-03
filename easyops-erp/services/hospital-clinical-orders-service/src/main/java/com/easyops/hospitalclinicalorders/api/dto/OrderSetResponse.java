package com.easyops.hospitalclinicalorders.api.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public class OrderSetResponse {
    private UUID id;
    private UUID patientId;
    private UUID visitId;
    private UUID orderingDoctorId;
    private UUID orderingDepartmentId;
    private String orderContext;
    private String priority;
    private UUID facilityId;
    private OffsetDateTime createdAt;
    private UUID createdBy;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getPatientId() { return patientId; }
    public void setPatientId(UUID patientId) { this.patientId = patientId; }
    public UUID getVisitId() { return visitId; }
    public void setVisitId(UUID visitId) { this.visitId = visitId; }
    public UUID getOrderingDoctorId() { return orderingDoctorId; }
    public void setOrderingDoctorId(UUID orderingDoctorId) { this.orderingDoctorId = orderingDoctorId; }
    public UUID getOrderingDepartmentId() { return orderingDepartmentId; }
    public void setOrderingDepartmentId(UUID orderingDepartmentId) { this.orderingDepartmentId = orderingDepartmentId; }
    public String getOrderContext() { return orderContext; }
    public void setOrderContext(String orderContext) { this.orderContext = orderContext; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public UUID getFacilityId() { return facilityId; }
    public void setFacilityId(UUID facilityId) { this.facilityId = facilityId; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }
}
