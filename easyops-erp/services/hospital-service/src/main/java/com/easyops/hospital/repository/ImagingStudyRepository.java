package com.easyops.hospital.repository;

import com.easyops.hospital.entity.ImagingStudy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ImagingStudyRepository extends JpaRepository<ImagingStudy, UUID> {
    
    List<ImagingStudy> findByPatientPatientId(UUID patientId);
    
    List<ImagingStudy> findByPatientPatientIdOrderByStudyDateDesc(UUID patientId);
    
    List<ImagingStudy> findByOrderOrderId(UUID orderId);
    
    List<ImagingStudy> findByEncounterId(UUID encounterId);
    
    Optional<ImagingStudy> findByStudyNumber(String studyNumber);
    
    Optional<ImagingStudy> findByAccessionNumber(String accessionNumber);

    Optional<ImagingStudy> findByDicomStudyInstanceUid(String dicomStudyInstanceUid);
    
    @Query("SELECT s FROM ImagingStudy s WHERE s.patient.patientId = :patientId " +
           "AND s.studyModality = :modality ORDER BY s.studyDate DESC")
    List<ImagingStudy> findStudiesByModality(
        @Param("patientId") UUID patientId,
        @Param("modality") ImagingStudy.StudyModality modality);
    
    @Query("SELECT s FROM ImagingStudy s WHERE s.patient.patientId = :patientId " +
           "AND s.bodyPartExamined = :bodyPart ORDER BY s.studyDate DESC")
    List<ImagingStudy> findStudiesByBodyPart(
        @Param("patientId") UUID patientId,
        @Param("bodyPart") String bodyPart);
    
    @Query("SELECT s FROM ImagingStudy s WHERE s.patient.patientId = :patientId " +
           "AND s.hasCriticalFindings = true AND s.isCriticalFindingAcknowledged = false " +
           "ORDER BY s.studyDate DESC")
    List<ImagingStudy> findUnacknowledgedCriticalFindings(@Param("patientId") UUID patientId);
    
    @Query("SELECT s FROM ImagingStudy s WHERE s.hasCriticalFindings = true " +
           "AND s.isCriticalFindingAcknowledged = false ORDER BY s.studyDate DESC")
    List<ImagingStudy> findAllUnacknowledgedCriticalFindings();
    
    @Query("SELECT s FROM ImagingStudy s WHERE s.patient.patientId = :patientId " +
           "AND s.isReviewed = false ORDER BY s.studyDate DESC")
    List<ImagingStudy> findUnreviewedStudies(@Param("patientId") UUID patientId);
    
    @Query("SELECT s FROM ImagingStudy s WHERE s.patient.patientId = :patientId " +
           "AND s.studyDate >= :startDate AND s.studyDate <= :endDate " +
           "ORDER BY s.studyDate DESC")
    List<ImagingStudy> findStudiesByDateRange(
        @Param("patientId") UUID patientId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT s FROM ImagingStudy s WHERE s.patient.patientId = :patientId " +
           "AND s.bodyPartExamined = :bodyPart AND s.studyDate < :beforeDate " +
           "ORDER BY s.studyDate DESC")
    List<ImagingStudy> findPriorStudiesByBodyPart(
        @Param("patientId") UUID patientId,
        @Param("bodyPart") String bodyPart,
        @Param("beforeDate") LocalDateTime beforeDate);
    
    @Query("SELECT s FROM ImagingStudy s WHERE s.patient.patientId = :patientId " +
           "AND s.studyModality = :modality AND s.studyDate < :beforeDate " +
           "ORDER BY s.studyDate DESC")
    List<ImagingStudy> findPriorStudiesByModality(
        @Param("patientId") UUID patientId,
        @Param("modality") ImagingStudy.StudyModality modality,
        @Param("beforeDate") LocalDateTime beforeDate);
    
    @Query("SELECT s FROM ImagingStudy s WHERE s.order.orderId = :orderId " +
           "ORDER BY s.studyDate DESC")
    List<ImagingStudy> findStudiesByOrder(@Param("orderId") UUID orderId);
    
    void deleteByPatientPatientId(UUID patientId);
}
