package com.easyops.hospital.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Configurable EP (Easy Prescription) reference/lookup data.
 * One row per (category, value) pair.
 * Categories: DOSAGE_FORM, DISEASE_CATEGORY, FREQUENCY, INSTRUCTION,
 *             REFERRAL, COMPLAINT, MEDICATION, ADVICE, TEST.
 */
@Entity
@Table(name = "ep_lookup_items", schema = "ehr",
        uniqueConstraints = @UniqueConstraint(name = "uq_ep_lookup_category_value", columnNames = {"category", "value"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EpLookupItem {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    private UUID id;

    @Column(name = "category", nullable = false, length = 50)
    private String category;

    @Column(name = "value", nullable = false, length = 1000)
    private String value;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
