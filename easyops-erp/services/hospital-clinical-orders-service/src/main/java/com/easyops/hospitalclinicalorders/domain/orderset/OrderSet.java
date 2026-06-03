package com.easyops.hospitalclinicalorders.domain.orderset;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "order_sets", schema = "hospital_clinical_orders")
@EntityListeners(AuditingEntityListener.class)
public class OrderSet {

    @Id
    private UUID id;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "visit_id")
    private UUID visitId;

    @Column(name = "ordering_doctor_id")
    private UUID orderingDoctorId;

    @Column(name = "ordering_department_id")
    private UUID orderingDepartmentId;

    @Column(name = "order_context", length = 20)
    private String orderContext;

    @Column(name = "priority", length = 20)
    private String priority;

    @Column(name = "facility_id")
    private UUID facilityId;

    @CreatedDate
    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @Column(name = "created_by")
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
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }
}
