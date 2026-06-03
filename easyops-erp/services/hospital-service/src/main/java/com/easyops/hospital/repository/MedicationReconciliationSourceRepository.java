package com.easyops.hospital.repository;

import com.easyops.hospital.entity.MedicationReconciliationSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MedicationReconciliationSourceRepository extends JpaRepository<MedicationReconciliationSource, UUID> {
    
    List<MedicationReconciliationSource> findByReconciliationReconciliationId(UUID reconciliationId);
    
    @Query("SELECT mrs FROM MedicationReconciliationSource mrs WHERE mrs.reconciliation.reconciliationId = :reconciliationId AND mrs.sourceType = :sourceType")
    List<MedicationReconciliationSource> findByReconciliationIdAndSourceType(@Param("reconciliationId") UUID reconciliationId, @Param("sourceType") MedicationReconciliationSource.SourceType sourceType);
}
