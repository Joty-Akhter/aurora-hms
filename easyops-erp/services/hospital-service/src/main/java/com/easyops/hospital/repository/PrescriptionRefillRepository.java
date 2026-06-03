package com.easyops.hospital.repository;

import com.easyops.hospital.entity.PrescriptionRefill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PrescriptionRefillRepository extends JpaRepository<PrescriptionRefill, UUID> {
    
    List<PrescriptionRefill> findByPrescriptionPrescriptionId(UUID prescriptionId);
    
    List<PrescriptionRefill> findByPrescriptionPrescriptionIdOrderByRefillDateDesc(UUID prescriptionId);
    
    List<PrescriptionRefill> findByRefillRequestRefillRequestId(UUID refillRequestId);
    
    @Query("SELECT r FROM PrescriptionRefill r WHERE r.prescription.patient.patientId = :patientId " +
           "ORDER BY r.refillDate DESC")
    List<PrescriptionRefill> findByPatient(@Param("patientId") UUID patientId);
    
    @Query("SELECT r FROM PrescriptionRefill r WHERE r.prescription.prescriptionId = :prescriptionId " +
           "ORDER BY r.refillNumber DESC")
    List<PrescriptionRefill> findByPrescriptionOrderByRefillNumber(@Param("prescriptionId") UUID prescriptionId);
    
    @Query("SELECT MAX(r.refillNumber) FROM PrescriptionRefill r WHERE r.prescription.prescriptionId = :prescriptionId")
    Optional<Integer> findMaxRefillNumberByPrescription(@Param("prescriptionId") UUID prescriptionId);
    
    @Query("SELECT r FROM PrescriptionRefill r WHERE r.prescription.prescriptionId = :prescriptionId " +
           "AND r.refillDate >= :startDate AND r.refillDate <= :endDate " +
           "ORDER BY r.refillDate DESC")
    List<PrescriptionRefill> findByPrescriptionAndDateRange(
        @Param("prescriptionId") UUID prescriptionId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate);
    
    @Query("SELECT r FROM PrescriptionRefill r WHERE r.pharmacyId = :pharmacyId " +
           "ORDER BY r.refillDate DESC")
    List<PrescriptionRefill> findByPharmacy(@Param("pharmacyId") UUID pharmacyId);
    
    void deleteByPrescriptionPrescriptionId(UUID prescriptionId);
}
