package com.easyops.hospitalscheduling.domain.resource;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "scheduling_booking_rules", schema = "hospital_scheduling")
@EntityListeners(AuditingEntityListener.class)
public class BookingRule {

    @Id
    private UUID id;

    @Column(name = "scope_type", nullable = false, length = 30)
    private String scopeType; // RESOURCE, BRANCH, GLOBAL

    @Column(name = "scope_id")
    private UUID scopeId;

    @Column(name = "cancellation_cutoff_hours")
    private Integer cancellationCutoffHours;

    @Column(name = "max_per_slot")
    private Integer maxPerSlot;

    @Column(name = "channel", length = 30)
    private String channel; // WEB, MOBILE, FRONT_DESK, CALL_CENTER, INTERNAL, null=all

    @Column(name = "channel_daily_cap")
    private Integer channelDailyCap;

    @Column(name = "max_advance_days")
    private Integer maxAdvanceDays;

    @CreatedDate
    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getScopeType() { return scopeType; }
    public void setScopeType(String scopeType) { this.scopeType = scopeType; }
    public UUID getScopeId() { return scopeId; }
    public void setScopeId(UUID scopeId) { this.scopeId = scopeId; }
    public Integer getCancellationCutoffHours() { return cancellationCutoffHours; }
    public void setCancellationCutoffHours(Integer cancellationCutoffHours) { this.cancellationCutoffHours = cancellationCutoffHours; }
    public Integer getMaxPerSlot() { return maxPerSlot; }
    public void setMaxPerSlot(Integer maxPerSlot) { this.maxPerSlot = maxPerSlot; }
    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
    public Integer getChannelDailyCap() { return channelDailyCap; }
    public void setChannelDailyCap(Integer channelDailyCap) { this.channelDailyCap = channelDailyCap; }
    public Integer getMaxAdvanceDays() { return maxAdvanceDays; }
    public void setMaxAdvanceDays(Integer maxAdvanceDays) { this.maxAdvanceDays = maxAdvanceDays; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
