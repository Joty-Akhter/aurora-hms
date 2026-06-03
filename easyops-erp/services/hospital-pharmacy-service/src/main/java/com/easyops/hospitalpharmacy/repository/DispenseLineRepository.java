package com.easyops.hospitalpharmacy.repository;

import com.easyops.hospitalpharmacy.entity.DispenseLine;
import com.easyops.hospitalpharmacy.entity.DispenseOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface DispenseLineRepository extends JpaRepository<DispenseLine, UUID> {

    List<DispenseLine> findByDispenseOrder(DispenseOrder order);

    /** Phase P4 — controlled substance register (WS-H3 / WS-L): issued lines in window. */
    @Query("""
            SELECT dl FROM DispenseLine dl
            JOIN FETCH dl.drug d
            JOIN FETCH dl.dispenseOrder o
            JOIN FETCH o.pharmacyLocation pl
            WHERE pl.id = :pharmacyId
            AND dl.createdAt >= :from
            AND dl.createdAt < :to
            AND dl.quantityDispensed > 0
            AND dl.status IN ('DISPENSED', 'PARTIALLY_DISPENSED', 'FILLED_WITH_STOCK_OVERRIDE')
            AND (d.controlledDrugFlag = true
                OR (d.controlledProfileCode IS NOT NULL AND LOWER(TRIM(d.controlledProfileCode)) <> 'none'))
            ORDER BY dl.createdAt
            """)
    List<DispenseLine> findControlledDispenseLinesForRegister(
            @Param("pharmacyId") UUID pharmacyId,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to);

    /** Phase P4 — stock override audit (WS-L1). */
    @Query("""
            SELECT dl FROM DispenseLine dl
            JOIN FETCH dl.drug d
            JOIN FETCH dl.dispenseOrder o
            JOIN FETCH o.pharmacyLocation pl
            WHERE pl.id = :pharmacyId
            AND dl.createdAt >= :from
            AND dl.createdAt < :to
            AND dl.status = 'FILLED_WITH_STOCK_OVERRIDE'
            ORDER BY dl.createdAt
            """)
    List<DispenseLine> findStockOverrideLinesForReport(
            @Param("pharmacyId") UUID pharmacyId,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to);

    /** Issued dispense lines in window — source of truth for consumption / sales reports. */
    @Query("""
            SELECT dl FROM DispenseLine dl
            JOIN FETCH dl.drug d
            JOIN FETCH dl.dispenseOrder o
            JOIN FETCH o.pharmacyLocation pl
            WHERE pl.id = :pharmacyId
            AND dl.createdAt >= :from
            AND dl.createdAt < :to
            AND dl.quantityDispensed > 0
            AND dl.status IN ('DISPENSED', 'PARTIALLY_DISPENSED', 'FILLED_WITH_STOCK_OVERRIDE')
            ORDER BY dl.createdAt
            """)
    List<DispenseLine> findDispensedLinesForConsumptionReport(
            @Param("pharmacyId") UUID pharmacyId,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to);
}

