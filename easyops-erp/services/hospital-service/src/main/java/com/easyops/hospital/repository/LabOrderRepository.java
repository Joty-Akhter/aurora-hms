package com.easyops.hospital.repository;

import com.easyops.hospital.entity.LabOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LabOrderRepository extends JpaRepository<LabOrder, UUID> {
    
    List<LabOrder> findByPatientPatientId(UUID patientId);
    
    List<LabOrder> findByPatientPatientIdOrderByOrderDateDesc(UUID patientId);
    
    List<LabOrder> findByPatientPatientIdAndOrderStatus(
        UUID patientId, LabOrder.OrderStatus status);
    
    List<LabOrder> findByEncounterId(UUID encounterId);
    
    List<LabOrder> findByOrderingProviderId(UUID providerId);
    
    Optional<LabOrder> findByOrderNumber(String orderNumber);
    
    @Query("SELECT o FROM LabOrder o WHERE o.patient.patientId = :patientId " +
           "AND o.orderStatus IN ('PENDING', 'SENT', 'COLLECTED', 'IN_PROCESS') " +
           "ORDER BY o.orderDate DESC")
    List<LabOrder> findPendingOrdersByPatient(@Param("patientId") UUID patientId);
    
    @Query("SELECT o FROM LabOrder o WHERE o.patient.patientId = :patientId " +
           "AND o.orderStatus = 'COMPLETED' ORDER BY o.orderDate DESC")
    List<LabOrder> findCompletedOrdersByPatient(@Param("patientId") UUID patientId);
    
    @Query("SELECT o FROM LabOrder o WHERE o.patient.patientId = :patientId " +
           "AND o.orderDate >= :startDate AND o.orderDate <= :endDate " +
           "ORDER BY o.orderDate DESC")
    List<LabOrder> findOrdersByDateRange(
        @Param("patientId") UUID patientId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT o FROM LabOrder o WHERE o.loincCode = :loincCode " +
           "AND o.patient.patientId = :patientId ORDER BY o.orderDate DESC")
    List<LabOrder> findOrdersByLoincCode(
        @Param("patientId") UUID patientId,
        @Param("loincCode") String loincCode);
    
    @Query("SELECT o FROM LabOrder o WHERE o.testCategory = :category " +
           "AND o.patient.patientId = :patientId ORDER BY o.orderDate DESC")
    List<LabOrder> findOrdersByCategory(
        @Param("patientId") UUID patientId,
        @Param("category") String category);
    
    @Query("SELECT o FROM LabOrder o WHERE o.orderStatus = :status " +
           "ORDER BY o.orderDate DESC")
    List<LabOrder> findOrdersByStatus(@Param("status") LabOrder.OrderStatus status);
    
    void deleteByPatientPatientId(UUID patientId);
}
