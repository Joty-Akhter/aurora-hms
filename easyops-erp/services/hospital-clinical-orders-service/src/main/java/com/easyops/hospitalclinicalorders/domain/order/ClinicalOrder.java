package com.easyops.hospitalclinicalorders.domain.order;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "clinical_orders", schema = "hospital_clinical_orders")
@EntityListeners(AuditingEntityListener.class)
public class ClinicalOrder {

    @Id
    private UUID id;

    @Column(name = "order_set_id", nullable = false, insertable = true, updatable = false)
    private UUID orderSetId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_set_id", insertable = false, updatable = false)
    private com.easyops.hospitalclinicalorders.domain.orderset.OrderSet orderSet;

    @Column(name = "order_type", nullable = false, length = 20)
    private String orderType;

    @Column(name = "item_code", nullable = false, length = 100)
    private String itemCode;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "priority", length = 20)
    private String priority;

    @Column(name = "ordering_notes", columnDefinition = "TEXT")
    private String orderingNotes;

    @Column(name = "performed_at")
    private OffsetDateTime performedAt;

    @Column(name = "performed_by")
    private UUID performedBy;

    @Column(name = "cancel_reason", columnDefinition = "TEXT")
    private String cancelReason;

    @Column(name = "cancelled_at")
    private OffsetDateTime cancelledAt;

    @Column(name = "cancelled_by")
    private UUID cancelledBy;

    @Column(name = "external_system_id", length = 255)
    private String externalSystemId;

    @Column(name = "result_status", length = 20)
    private String resultStatus;

    @Column(name = "result_available_at")
    private OffsetDateTime resultAvailableAt;

    @CreatedDate
    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "facility_id")
    private UUID facilityId;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getOrderSetId() { return orderSetId; }
    public void setOrderSetId(UUID orderSetId) { this.orderSetId = orderSetId; }
    public String getOrderType() { return orderType; }
    public void setOrderType(String orderType) { this.orderType = orderType; }
    public String getItemCode() { return itemCode; }
    public void setItemCode(String itemCode) { this.itemCode = itemCode; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public String getOrderingNotes() { return orderingNotes; }
    public void setOrderingNotes(String orderingNotes) { this.orderingNotes = orderingNotes; }
    public OffsetDateTime getPerformedAt() { return performedAt; }
    public void setPerformedAt(OffsetDateTime performedAt) { this.performedAt = performedAt; }
    public UUID getPerformedBy() { return performedBy; }
    public void setPerformedBy(UUID performedBy) { this.performedBy = performedBy; }
    public String getCancelReason() { return cancelReason; }
    public void setCancelReason(String cancelReason) { this.cancelReason = cancelReason; }
    public OffsetDateTime getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(OffsetDateTime cancelledAt) { this.cancelledAt = cancelledAt; }
    public UUID getCancelledBy() { return cancelledBy; }
    public void setCancelledBy(UUID cancelledBy) { this.cancelledBy = cancelledBy; }
    public String getExternalSystemId() { return externalSystemId; }
    public void setExternalSystemId(String externalSystemId) { this.externalSystemId = externalSystemId; }
    public String getResultStatus() { return resultStatus; }
    public void setResultStatus(String resultStatus) { this.resultStatus = resultStatus; }
    public OffsetDateTime getResultAvailableAt() { return resultAvailableAt; }
    public void setResultAvailableAt(OffsetDateTime resultAvailableAt) { this.resultAvailableAt = resultAvailableAt; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }
    public UUID getFacilityId() { return facilityId; }
    public void setFacilityId(UUID facilityId) { this.facilityId = facilityId; }
}
