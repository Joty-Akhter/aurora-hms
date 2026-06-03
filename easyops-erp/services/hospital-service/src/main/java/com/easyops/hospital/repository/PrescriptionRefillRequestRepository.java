package com.easyops.hospital.repository;

import com.easyops.hospital.entity.PrescriptionRefillRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface PrescriptionRefillRequestRepository extends JpaRepository<PrescriptionRefillRequest, UUID> {
    
    List<PrescriptionRefillRequest> findByPrescriptionPrescriptionId(UUID prescriptionId);
    
    List<PrescriptionRefillRequest> findByPrescriptionPrescriptionIdOrderByRequestDateDesc(UUID prescriptionId);
    
    List<PrescriptionRefillRequest> findByRequestStatus(PrescriptionRefillRequest.RequestStatus status);
    
    List<PrescriptionRefillRequest> findByRequestSource(PrescriptionRefillRequest.RequestSource source);
    
    @Query("SELECT r FROM PrescriptionRefillRequest r WHERE r.requestStatus = 'PENDING' " +
           "ORDER BY " +
           "CASE r.urgencyLevel " +
           "WHEN 'URGENT' THEN 1 " +
           "WHEN 'HIGH' THEN 2 " +
           "WHEN 'MEDIUM' THEN 3 " +
           "WHEN 'LOW' THEN 4 " +
           "ELSE 5 END, r.requestDate ASC")
    List<PrescriptionRefillRequest> findPendingRequestsOrderedByUrgency();
    
    @Query("SELECT r FROM PrescriptionRefillRequest r WHERE r.prescription.patient.patientId = :patientId " +
           "ORDER BY r.requestDate DESC")
    List<PrescriptionRefillRequest> findByPatient(@Param("patientId") UUID patientId);
    
    @Query("SELECT r FROM PrescriptionRefillRequest r WHERE r.prescription.patient.patientId = :patientId " +
           "AND r.requestStatus = :status ORDER BY r.requestDate DESC")
    List<PrescriptionRefillRequest> findByPatientAndStatus(
        @Param("patientId") UUID patientId,
        @Param("status") PrescriptionRefillRequest.RequestStatus status);
    
    @Query("SELECT r FROM PrescriptionRefillRequest r WHERE r.pharmacyId = :pharmacyId " +
           "ORDER BY r.requestDate DESC")
    List<PrescriptionRefillRequest> findByPharmacy(@Param("pharmacyId") UUID pharmacyId);
    
    @Query("SELECT r FROM PrescriptionRefillRequest r WHERE r.wasAutoApproved = true " +
           "ORDER BY r.approvedDate DESC")
    List<PrescriptionRefillRequest> findAutoApprovedRequests();
    
    @Query("SELECT COUNT(r) FROM PrescriptionRefillRequest r WHERE r.requestStatus = 'PENDING'")
    Long countPendingRequests();
    
    void deleteByPrescriptionPrescriptionId(UUID prescriptionId);
}
