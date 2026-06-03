package com.easyops.hospital.repository;

import com.easyops.hospital.entity.ImagingCriticalFindingAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ImagingCriticalFindingAlertRepository extends JpaRepository<ImagingCriticalFindingAlert, UUID> {
    
    List<ImagingCriticalFindingAlert> findByStudyStudyId(UUID studyId);
    
    List<ImagingCriticalFindingAlert> findByPatientPatientId(UUID patientId);
    
    @Query("SELECT a FROM ImagingCriticalFindingAlert a WHERE a.patient.patientId = :patientId " +
           "AND a.isAcknowledged = false ORDER BY a.createdAt DESC")
    List<ImagingCriticalFindingAlert> findUnacknowledgedAlertsByPatient(@Param("patientId") UUID patientId);
    
    @Query("SELECT a FROM ImagingCriticalFindingAlert a WHERE a.isAcknowledged = false " +
           "ORDER BY a.createdAt DESC")
    List<ImagingCriticalFindingAlert> findAllUnacknowledgedAlerts();
    
    @Query("SELECT a FROM ImagingCriticalFindingAlert a WHERE a.alertStatus = :status " +
           "ORDER BY a.createdAt DESC")
    List<ImagingCriticalFindingAlert> findByAlertStatus(@Param("status") ImagingCriticalFindingAlert.AlertStatus status);
    
    Optional<ImagingCriticalFindingAlert> findByStudyStudyIdAndAlertStatus(
        UUID studyId, 
        ImagingCriticalFindingAlert.AlertStatus status);
}
