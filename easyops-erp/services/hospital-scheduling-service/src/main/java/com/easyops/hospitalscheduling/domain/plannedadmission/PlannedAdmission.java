package com.easyops.hospitalscheduling.domain.plannedadmission;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "scheduling_planned_admissions", schema = "hospital_scheduling")
@EntityListeners(AuditingEntityListener.class)
public class PlannedAdmission {

    @Id
    private UUID id;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "preferred_date", nullable = false)
    private LocalDate preferredDate;

    @Column(name = "preferred_ward_or_bed_class", length = 100)
    private String preferredWardOrBedClass;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(name = "bed_group_resource_id")
    private UUID bedGroupResourceId;

    @Column(name = "reservation_id")
    private UUID reservationId;

    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;

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
    public LocalDate getPreferredDate() { return preferredDate; }
    public void setPreferredDate(LocalDate preferredDate) { this.preferredDate = preferredDate; }
    public String getPreferredWardOrBedClass() { return preferredWardOrBedClass; }
    public void setPreferredWardOrBedClass(String preferredWardOrBedClass) { this.preferredWardOrBedClass = preferredWardOrBedClass; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public UUID getBedGroupResourceId() { return bedGroupResourceId; }
    public void setBedGroupResourceId(UUID bedGroupResourceId) { this.bedGroupResourceId = bedGroupResourceId; }
    public UUID getReservationId() { return reservationId; }
    public void setReservationId(UUID reservationId) { this.reservationId = reservationId; }
    public OffsetDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(OffsetDateTime expiresAt) { this.expiresAt = expiresAt; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }
}
