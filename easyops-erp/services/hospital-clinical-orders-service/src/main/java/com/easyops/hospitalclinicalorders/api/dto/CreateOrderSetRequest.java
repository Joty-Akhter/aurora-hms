package com.easyops.hospitalclinicalorders.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public class CreateOrderSetRequest {
    @NotNull
    private UUID patientId;
    private UUID visitId;
    private UUID orderingDoctorId;
    private UUID orderingDepartmentId;
    private String orderContext; // OPD, IPD, ED
    private String priority;     // STAT, ROUTINE, URGENT
    private UUID facilityId;     // optional, for multi-facility
    @Valid
    @NotNull
    private List<CreateOrderLineRequest> orders;

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
    public List<CreateOrderLineRequest> getOrders() { return orders; }
    public void setOrders(List<CreateOrderLineRequest> orders) { this.orders = orders; }
}
