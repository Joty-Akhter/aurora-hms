package com.easyops.hospitalclinicalorders.domain.worklist;

import jakarta.persistence.*;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "order_worklist_items", schema = "hospital_clinical_orders")
@EntityListeners(AuditingEntityListener.class)
public class OrderWorklistItem {

    @Id
    private UUID id;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", insertable = false, updatable = false)
    private com.easyops.hospitalclinicalorders.domain.order.ClinicalOrder order;

    @Column(name = "worklist_type", nullable = false, length = 50)
    private String worklistType;

    @Column(name = "assigned_to_user_id")
    private UUID assignedToUserId;

    @Column(name = "assigned_to_role", length = 100)
    private String assignedToRole;

    @Column(name = "scheduled_time")
    private OffsetDateTime scheduledTime;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getOrderId() { return orderId; }
    public void setOrderId(UUID orderId) { this.orderId = orderId; }
    public String getWorklistType() { return worklistType; }
    public void setWorklistType(String worklistType) { this.worklistType = worklistType; }
    public UUID getAssignedToUserId() { return assignedToUserId; }
    public void setAssignedToUserId(UUID assignedToUserId) { this.assignedToUserId = assignedToUserId; }
    public String getAssignedToRole() { return assignedToRole; }
    public void setAssignedToRole(String assignedToRole) { this.assignedToRole = assignedToRole; }
    public OffsetDateTime getScheduledTime() { return scheduledTime; }
    public void setScheduledTime(OffsetDateTime scheduledTime) { this.scheduledTime = scheduledTime; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
