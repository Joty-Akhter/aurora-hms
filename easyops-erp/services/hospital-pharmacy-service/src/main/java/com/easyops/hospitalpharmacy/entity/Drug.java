package com.easyops.hospitalpharmacy.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "drugs", schema = "hospital_pharmacy")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Drug {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "generic_name", nullable = false, length = 255)
    private String genericName;

    @Column(name = "brand_name", length = 255)
    private String brandName;

    @Column(name = "strength", length = 100)
    private String strength;

    @Column(name = "form", length = 100)
    private String form;

    @Column(name = "route", length = 100)
    private String route;

    @Column(name = "pack_size", length = 50)
    private String packSize;

    @Column(name = "unit_of_measure", length = 50)
    private String unitOfMeasure;

    @Column(name = "therapeutic_class_id")
    private UUID therapeuticClassId;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "controlled_drug_flag", nullable = false)
    @Builder.Default
    private boolean controlledDrugFlag = false;

    /**
     * Optional jurisdiction profile (e.g. US_DEA_II). When set and not {@code NONE}, the drug is treated as
     * controlled for policy checks even if {@link #controlledDrugFlag} is false (H1 — Phase P4).
     */
    @Column(name = "controlled_profile_code", length = 64)
    private String controlledProfileCode;

    @Column(name = "batch_required", nullable = false)
    @Builder.Default
    private boolean batchRequired = true;

    @Column(name = "expiry_required", nullable = false)
    @Builder.Default
    private boolean expiryRequired = true;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "manufacturer_id", nullable = false)
    private Manufacturer manufacturer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_group_id")
    private ProductGroup productGroup;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dispensing_unit_id")
    private Unit dispensingUnit;

    @Column(name = "mrp", precision = 10, scale = 2)
    private BigDecimal mrp;

    @Column(name = "sale_price", precision = 10, scale = 2)
    private BigDecimal salePrice;

    @Column(name = "purchase_price", precision = 10, scale = 2)
    private BigDecimal purchasePrice;

    @Column(name = "rack_no", length = 50)
    private String rackNo;

    @Column(name = "reminder_stock", precision = 19, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal reminderStock = BigDecimal.ZERO;

    @Column(name = "hsn_code", length = 50)
    private String hsnCode;

    @Column(name = "product_code", length = 100)
    private String productCode;

    @Column(name = "department_id")
    private UUID departmentId;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    /** True if legacy flag or a non-empty, non-{@code NONE} profile code is set. */
    public boolean isControlledSubstance() {
        if (controlledDrugFlag) {
            return true;
        }
        if (controlledProfileCode == null || controlledProfileCode.isBlank()) {
            return false;
        }
        return !"NONE".equalsIgnoreCase(controlledProfileCode.trim());
    }
}

