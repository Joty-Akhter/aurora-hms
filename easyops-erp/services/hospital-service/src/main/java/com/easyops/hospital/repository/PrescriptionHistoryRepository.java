package com.easyops.hospital.repository;

import com.easyops.hospital.entity.PrescriptionHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PrescriptionHistoryRepository extends JpaRepository<PrescriptionHistory, UUID> {
    
    List<PrescriptionHistory> findByPrescriptionPrescriptionId(UUID prescriptionId);
    
    List<PrescriptionHistory> findByPrescriptionPrescriptionIdOrderByChangedDateDesc(UUID prescriptionId);
    
    List<PrescriptionHistory> findByPrescriptionPrescriptionIdAndChangeType(
        UUID prescriptionId, PrescriptionHistory.ChangeType changeType);
    
    @Query("SELECT h FROM PrescriptionHistory h WHERE h.prescription.prescriptionId = :prescriptionId " +
           "ORDER BY h.changedDate DESC")
    List<PrescriptionHistory> findHistoryByPrescription(@Param("prescriptionId") UUID prescriptionId);
    
    void deleteByPrescriptionPrescriptionId(UUID prescriptionId);
}
