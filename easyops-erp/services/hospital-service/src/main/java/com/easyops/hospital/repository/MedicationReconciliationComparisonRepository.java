package com.easyops.hospital.repository;

import com.easyops.hospital.entity.MedicationReconciliationComparison;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MedicationReconciliationComparisonRepository extends JpaRepository<MedicationReconciliationComparison, UUID> {
    
    List<MedicationReconciliationComparison> findByReconciliationReconciliationId(UUID reconciliationId);
    
    @Query("SELECT mrc FROM MedicationReconciliationComparison mrc WHERE mrc.reconciliation.reconciliationId = :reconciliationId AND mrc.comparisonStatus = :status")
    List<MedicationReconciliationComparison> findByReconciliationIdAndStatus(@Param("reconciliationId") UUID reconciliationId, @Param("status") MedicationReconciliationComparison.ComparisonStatus status);
    
    @Query("SELECT mrc FROM MedicationReconciliationComparison mrc WHERE mrc.reconciliation.reconciliationId = :reconciliationId AND mrc.actionTaken = :action")
    List<MedicationReconciliationComparison> findByReconciliationIdAndAction(@Param("reconciliationId") UUID reconciliationId, @Param("action") MedicationReconciliationComparison.ActionTaken action);
}
