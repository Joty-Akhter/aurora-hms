package com.easyops.hospital.repository;

import com.easyops.hospital.entity.LabCriticalValueAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LabCriticalValueAlertRepository extends JpaRepository<LabCriticalValueAlert, UUID> {
    
    List<LabCriticalValueAlert> findByResultResultId(UUID resultId);
    
    List<LabCriticalValueAlert> findByPatientPatientId(UUID patientId);
    
    @Query("SELECT a FROM LabCriticalValueAlert a WHERE a.isAcknowledged = false " +
           "ORDER BY a.createdAt DESC")
    List<LabCriticalValueAlert> findUnacknowledgedAlerts();
    
    @Query("SELECT a FROM LabCriticalValueAlert a WHERE a.patient.patientId = :patientId " +
           "AND a.isAcknowledged = false ORDER BY a.createdAt DESC")
    List<LabCriticalValueAlert> findUnacknowledgedAlertsByPatient(@Param("patientId") UUID patientId);
    
    @Query("SELECT a FROM LabCriticalValueAlert a WHERE a.alertStatus = :status " +
           "ORDER BY a.createdAt DESC")
    List<LabCriticalValueAlert> findByAlertStatus(@Param("status") LabCriticalValueAlert.AlertStatus status);
    
    @Query("SELECT a FROM LabCriticalValueAlert a WHERE a.notifiedProviderId = :providerId " +
           "AND a.isAcknowledged = false ORDER BY a.createdAt DESC")
    List<LabCriticalValueAlert> findUnacknowledgedAlertsByProvider(@Param("providerId") UUID providerId);
}
