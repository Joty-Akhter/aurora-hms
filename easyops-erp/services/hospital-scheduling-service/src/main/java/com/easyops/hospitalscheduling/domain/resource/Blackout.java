package com.easyops.hospitalscheduling.domain.resource;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "scheduling_blackouts", schema = "hospital_scheduling")
@EntityListeners(AuditingEntityListener.class)
public class Blackout {

    @Id
    private UUID id;

    @Column(name = "resource_id")
    private UUID resourceId;

    @Column(name = "branch_id")
    private UUID branchId;

    @Column(name = "blackout_date", nullable = false)
    private LocalDate blackoutDate;

    @Column(name = "reason", length = 255)
    private String reason;

    @CreatedDate
    @Column(name = "created_at")
    private OffsetDateTime createdAt;

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
    public UUID getResourceId() { return resourceId; }
    public void setResourceId(UUID resourceId) { this.resourceId = resourceId; }
    public UUID getBranchId() { return branchId; }
    public void setBranchId(UUID branchId) { this.branchId = branchId; }
    public LocalDate getBlackoutDate() { return blackoutDate; }
    public void setBlackoutDate(LocalDate blackoutDate) { this.blackoutDate = blackoutDate; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }
}
