package com.easyops.hospitalscheduling.domain.waitlist;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "scheduling_waitlist_entries", schema = "hospital_scheduling")
@EntityListeners(AuditingEntityListener.class)
public class WaitlistEntry {

    @Id
    private UUID id;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "resource_id", nullable = false)
    private UUID resourceId;

    @Column(name = "preferred_from_date")
    private LocalDate preferredFromDate;

    @Column(name = "preferred_to_date")
    private LocalDate preferredToDate;

    @Column(name = "priority", nullable = false)
    private int priority = 0;

    @Column(name = "priority_reason", length = 50)
    private String priorityReason;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @CreatedDate
    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @Column(name = "created_by")
    private UUID createdBy;

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getPatientId() { return patientId; }
    public void setPatientId(UUID patientId) { this.patientId = patientId; }
    public UUID getResourceId() { return resourceId; }
    public void setResourceId(UUID resourceId) { this.resourceId = resourceId; }
    public LocalDate getPreferredFromDate() { return preferredFromDate; }
    public void setPreferredFromDate(LocalDate preferredFromDate) { this.preferredFromDate = preferredFromDate; }
    public LocalDate getPreferredToDate() { return preferredToDate; }
    public void setPreferredToDate(LocalDate preferredToDate) { this.preferredToDate = preferredToDate; }
    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }
    public String getPriorityReason() { return priorityReason; }
    public void setPriorityReason(String priorityReason) { this.priorityReason = priorityReason; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }
}
