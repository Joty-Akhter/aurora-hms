package com.easyops.hospital.repository;

import com.easyops.hospital.entity.DrugLabInteractionAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DrugLabInteractionAlertRepository extends JpaRepository<DrugLabInteractionAlert, UUID> {
    
    List<DrugLabInteractionAlert> findByResultResultId(UUID resultId);
    
    List<DrugLabInteractionAlert> findByPrescriptionPrescriptionId(UUID prescriptionId);
    
    List<DrugLabInteractionAlert> findByPatientPatientId(UUID patientId);
    
    @Query("SELECT alert FROM DrugLabInteractionAlert alert WHERE alert.alertStatus = 'ACTIVE'")
    List<DrugLabInteractionAlert> findActiveAlerts();
    
    @Query("SELECT alert FROM DrugLabInteractionAlert alert WHERE alert.patient.patientId = :patientId AND alert.alertStatus = 'ACTIVE'")
    List<DrugLabInteractionAlert> findActiveAlertsByPatient(@Param("patientId") UUID patientId);
    
    @Query("SELECT alert FROM DrugLabInteractionAlert alert WHERE alert.result.resultId = :resultId AND alert.alertStatus = 'ACTIVE'")
    List<DrugLabInteractionAlert> findActiveAlertsByResult(@Param("resultId") UUID resultId);
    
    @Query("SELECT alert FROM DrugLabInteractionAlert alert WHERE alert.interactionSeverity IN ('HIGH', 'CRITICAL') AND alert.alertStatus = 'ACTIVE'")
    List<DrugLabInteractionAlert> findCriticalAlerts();
    
    @Query("SELECT alert FROM DrugLabInteractionAlert alert WHERE alert.organizationId = :organizationId")
    List<DrugLabInteractionAlert> findByOrganizationId(@Param("organizationId") UUID organizationId);
}
