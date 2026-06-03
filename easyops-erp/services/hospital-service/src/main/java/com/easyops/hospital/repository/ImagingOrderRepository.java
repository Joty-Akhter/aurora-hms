package com.easyops.hospital.repository;

import com.easyops.hospital.entity.ImagingOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ImagingOrderRepository extends JpaRepository<ImagingOrder, UUID> {
    
    List<ImagingOrder> findByPatientPatientId(UUID patientId);
    
    List<ImagingOrder> findByPatientPatientIdOrderByOrderDateDesc(UUID patientId);
    
    List<ImagingOrder> findByPatientPatientIdAndOrderStatus(
        UUID patientId, ImagingOrder.OrderStatus status);
    
    List<ImagingOrder> findByEncounterId(UUID encounterId);
    
    List<ImagingOrder> findByOrderingProviderId(UUID providerId);
    
    Optional<ImagingOrder> findByOrderNumber(String orderNumber);
    
    @Query("SELECT o FROM ImagingOrder o WHERE o.patient.patientId = :patientId " +
           "AND o.orderStatus IN ('PENDING', 'SENT', 'SCHEDULED', 'IN_PROGRESS') " +
           "ORDER BY o.orderDate DESC")
    List<ImagingOrder> findPendingOrdersByPatient(@Param("patientId") UUID patientId);
    
    @Query("SELECT o FROM ImagingOrder o WHERE o.patient.patientId = :patientId " +
           "AND o.orderStatus = 'COMPLETED' ORDER BY o.orderDate DESC")
    List<ImagingOrder> findCompletedOrdersByPatient(@Param("patientId") UUID patientId);
    
    @Query("SELECT o FROM ImagingOrder o WHERE o.patient.patientId = :patientId " +
           "AND o.orderDate >= :startDate AND o.orderDate <= :endDate " +
           "ORDER BY o.orderDate DESC")
    List<ImagingOrder> findOrdersByDateRange(
        @Param("patientId") UUID patientId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT o FROM ImagingOrder o WHERE o.studyModality = :modality " +
           "AND o.patient.patientId = :patientId ORDER BY o.orderDate DESC")
    List<ImagingOrder> findOrdersByModality(
        @Param("patientId") UUID patientId,
        @Param("modality") ImagingOrder.StudyModality modality);
    
    @Query("SELECT o FROM ImagingOrder o WHERE o.bodyPart = :bodyPart " +
           "AND o.patient.patientId = :patientId ORDER BY o.orderDate DESC")
    List<ImagingOrder> findOrdersByBodyPart(
        @Param("patientId") UUID patientId,
        @Param("bodyPart") String bodyPart);
    
    @Query("SELECT o FROM ImagingOrder o WHERE o.orderStatus = :status " +
           "ORDER BY o.orderDate DESC")
    List<ImagingOrder> findOrdersByStatus(@Param("status") ImagingOrder.OrderStatus status);
    
    void deleteByPatientPatientId(UUID patientId);
}
