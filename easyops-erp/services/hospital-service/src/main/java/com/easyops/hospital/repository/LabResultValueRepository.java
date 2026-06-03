package com.easyops.hospital.repository;

import com.easyops.hospital.entity.LabResultValue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LabResultValueRepository extends JpaRepository<LabResultValue, UUID> {
    
    List<LabResultValue> findByResultResultId(UUID resultId);
    
    List<LabResultValue> findByOrderOrderId(UUID orderId);
    
    List<LabResultValue> findByPatientPatientId(UUID patientId);
    
    @Query("SELECT lrv FROM LabResultValue lrv WHERE lrv.result.resultId = :resultId ORDER BY lrv.sequenceNumber ASC")
    List<LabResultValue> findByResultIdOrderedBySequence(@Param("resultId") UUID resultId);
    
    @Query("SELECT lrv FROM LabResultValue lrv WHERE lrv.order.orderId = :orderId ORDER BY lrv.sequenceNumber ASC")
    List<LabResultValue> findByOrderIdOrderedBySequence(@Param("orderId") UUID orderId);
    
    @Query("SELECT lrv FROM LabResultValue lrv WHERE lrv.organizationId = :organizationId")
    List<LabResultValue> findByOrganizationId(@Param("organizationId") UUID organizationId);
}
