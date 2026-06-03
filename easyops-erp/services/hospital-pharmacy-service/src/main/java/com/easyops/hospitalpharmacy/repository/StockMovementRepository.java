package com.easyops.hospitalpharmacy.repository;

import com.easyops.hospitalpharmacy.entity.PharmacyLocation;
import com.easyops.hospitalpharmacy.entity.StockMovement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface StockMovementRepository extends JpaRepository<StockMovement, UUID> {

    List<StockMovement> findByPharmacyLocationOrderByMovementTimeDesc(PharmacyLocation pharmacyLocation);

    List<StockMovement> findByPharmacyLocationAndMovementTimeBetweenAndMovementType(
            PharmacyLocation pharmacyLocation,
            OffsetDateTime from,
            OffsetDateTime to,
            String movementType
    );

    @Query("""
            SELECT sm FROM StockMovement sm
            WHERE sm.pharmacyLocation.id = :locationId
            AND (:drugId IS NULL OR sm.drug.id = :drugId)
            AND (:batchNumber IS NULL OR sm.batchNumber = :batchNumber)
            AND (:movementType IS NULL OR sm.movementType = :movementType)
            AND (:from IS NULL OR sm.movementTime >= :from)
            AND (:to IS NULL OR sm.movementTime <= :to)
            ORDER BY sm.movementTime DESC
            """)
    Page<StockMovement> findMovementsFiltered(
            @Param("locationId") UUID locationId,
            @Param("drugId") UUID drugId,
            @Param("batchNumber") String batchNumber,
            @Param("movementType") String movementType,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to,
            Pageable pageable);

    @Query("""
            SELECT sm FROM StockMovement sm
            WHERE sm.pharmacyLocation.id = :locationId
            AND sm.movementType = 'adjustment'
            AND (:from IS NULL OR sm.movementTime >= :from)
            AND (:to IS NULL OR sm.movementTime <= :to)
            ORDER BY sm.movementTime DESC
            """)
    List<StockMovement> findAdjustmentsForReport(
            @Param("locationId") UUID locationId,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to);
}
