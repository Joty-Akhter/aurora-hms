package com.easyops.hospitalpharmacy.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "pharmacy_locations", schema = "hospital_pharmacy")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PharmacyLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "type", nullable = false, length = 50)
    private String type; // OPD, IPD, store, ward_store

    @Column(name = "workflow_type", length = 50)
    private String workflowType; // SUPPLIER, CENTRAL_STORE, OUTLET_PHARMACY

    @Column(name = "is_24x7", nullable = false)
    @Builder.Default
    private boolean is24x7 = false;

    @Column(name = "operational_hours", columnDefinition = "TEXT")
    private String operationalHours;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}

