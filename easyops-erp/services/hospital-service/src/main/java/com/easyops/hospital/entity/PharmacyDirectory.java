package com.easyops.hospital.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * FR-P3.5: Retail / mail-order / e-prescribing pharmacy master data.
 * {@code ehr.prescriptions.pharmacy_id} is a soft reference to this table;
 * the prescription row also snapshots the name/NPI/phone for legal immutability.
 */
@Entity
@Table(name = "pharmacy_directory", schema = "hospital_pharmacy")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class PharmacyDirectory {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    private UUID id;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    /** CMS-assigned 10-digit Organisation NPI (NPI-2). */
    @Column(name = "npi", length = 10, unique = true)
    private String npi;

    /** NCPDP / Surescripts pharmacy identifier (up to 7 digits). */
    @Column(name = "ncpdp_id", length = 15)
    private String ncpdpId;

    @Column(name = "address_line1", length = 255)
    private String addressLine1;

    @Column(name = "address_line2", length = 255)
    private String addressLine2;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "state", length = 50)
    private String state;

    @Column(name = "zip", length = 20)
    private String zip;

    @Column(name = "country", nullable = false, length = 100)
    @Builder.Default
    private String country = "US";

    @Column(name = "phone", length = 30)
    private String phone;

    @Column(name = "fax", length = 30)
    private String fax;

    @Column(name = "email", length = 255)
    private String email;

    /** TRUE when enrolled in an e-prescribing network. */
    @Column(name = "is_eprescribing_capable", nullable = false)
    @Builder.Default
    private Boolean isEprescribingCapable = false;

    /** Network identifier when {@code isEprescribingCapable} is true (e.g. SURESCRIPTS). */
    @Column(name = "eprescribing_network", length = 100)
    private String eprescribingNetwork;

    /**
     * How the record was sourced.
     * @see DataSource
     */
    @Column(name = "data_source", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private DataSource dataSource = DataSource.MANUAL;

    /**
     * Timestamp of the last data-quality verification.
     * Records with {@code last_verified_at} older than 90 days are considered stale.
     */
    @Column(name = "last_verified_at")
    private OffsetDateTime lastVerifiedAt;

    @Column(name = "verification_notes", columnDefinition = "TEXT")
    private String verificationNotes;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "updated_by")
    private UUID updatedBy;

    public enum DataSource {
        MANUAL, NCPDP_FEED, SURESCRIPTS, IMPORTED
    }
}
