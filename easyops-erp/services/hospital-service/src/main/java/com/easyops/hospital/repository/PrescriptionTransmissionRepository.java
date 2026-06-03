package com.easyops.hospital.repository;

import com.easyops.hospital.entity.PrescriptionTransmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PrescriptionTransmissionRepository extends JpaRepository<PrescriptionTransmission, UUID> {
    
    /**
     * Find all transmissions for a prescription
     */
    List<PrescriptionTransmission> findByPrescriptionPrescriptionIdOrderByTransmissionDateDesc(UUID prescriptionId);
    
    /**
     * Find the most recent transmission for a prescription
     */
    Optional<PrescriptionTransmission> findFirstByPrescriptionPrescriptionIdOrderByTransmissionDateDesc(UUID prescriptionId);
    
    /**
     * Find successful transmissions for a prescription
     */
    @Query("SELECT t FROM PrescriptionTransmission t WHERE t.prescription.prescriptionId = :prescriptionId " +
           "AND t.transmissionSuccess = true ORDER BY t.transmissionDate DESC")
    List<PrescriptionTransmission> findSuccessfulTransmissionsByPrescription(@Param("prescriptionId") UUID prescriptionId);
    
    /**
     * Find transmissions by status
     */
    List<PrescriptionTransmission> findByTransmissionStatusOrderByTransmissionDateDesc(
        PrescriptionTransmission.TransmissionStatus status);
    
    /**
     * Find transmissions by network transaction ID
     */
    Optional<PrescriptionTransmission> findByNetworkTransactionId(String networkTransactionId);
    
    /**
     * Find pending transmissions that need retry
     */
    @Query("SELECT t FROM PrescriptionTransmission t WHERE t.transmissionStatus = 'PENDING' " +
           "OR (t.transmissionStatus = 'FAILED' AND t.retryCount < t.maxRetries) " +
           "ORDER BY t.transmissionDate ASC")
    List<PrescriptionTransmission> findPendingOrFailedTransmissions();
    
    /**
     * Find transmissions that need fill status update
     */
    @Query("SELECT t FROM PrescriptionTransmission t WHERE t.transmissionSuccess = true " +
           "AND t.confirmationReceived = true " +
           "AND (t.fillStatus IS NULL OR t.fillStatus NOT IN ('FILLED', 'PICKED_UP', 'CANCELLED', 'EXPIRED')) " +
           "ORDER BY t.transmissionDate ASC")
    List<PrescriptionTransmission> findTransmissionsNeedingFillStatusUpdate();
    
    /**
     * Find transmissions by pharmacy NPI
     */
    List<PrescriptionTransmission> findByPharmacyNpiOrderByTransmissionDateDesc(String pharmacyNpi);
    
    /**
     * Find transmissions by network name
     */
    List<PrescriptionTransmission> findByNetworkNameOrderByTransmissionDateDesc(String networkName);
}
