package com.easyops.hospital.repository;

import com.easyops.hospital.entity.PatientProblem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PatientProblemRepository extends JpaRepository<PatientProblem, UUID> {
    
    List<PatientProblem> findByPatientPatientId(UUID patientId);
    
    List<PatientProblem> findByPatientPatientIdOrderByDocumentedDateDesc(UUID patientId);
    
    List<PatientProblem> findByPatientPatientIdAndStatus(UUID patientId, PatientProblem.ProblemStatus status);
    
    List<PatientProblem> findByPatientPatientIdAndProblemType(UUID patientId, PatientProblem.ProblemType problemType);
    
    List<PatientProblem> findByPatientPatientIdAndStatusAndProblemType(
        UUID patientId, PatientProblem.ProblemStatus status, PatientProblem.ProblemType problemType);

    Optional<PatientProblem> findByProblemIdAndPatientPatientId(UUID problemId, UUID patientId);
    
    List<PatientProblem> findByEncounterId(UUID encounterId);
    
    List<PatientProblem> findByDocumentedBy(UUID documentedBy);
    
    @Query("SELECT p FROM PatientProblem p WHERE p.patient.patientId = :patientId " +
           "AND p.status = 'ACTIVE' ORDER BY p.priority DESC, p.documentedDate DESC")
    List<PatientProblem> findActiveProblemsByPatient(@Param("patientId") UUID patientId);
    
    @Query("SELECT p FROM PatientProblem p WHERE p.patient.patientId = :patientId " +
           "AND p.status = 'RESOLVED' ORDER BY p.resolvedDate DESC")
    List<PatientProblem> findResolvedProblemsByPatient(@Param("patientId") UUID patientId);
    
    @Query("SELECT p FROM PatientProblem p WHERE p.patient.patientId = :patientId " +
           "AND p.status IN ('ACTIVE', 'CHRONIC') ORDER BY p.priority DESC, p.documentedDate DESC")
    List<PatientProblem> findCurrentProblemsByPatient(@Param("patientId") UUID patientId);
    
    @Query("SELECT p FROM PatientProblem p WHERE p.patient.patientId = :patientId " +
           "AND p.priority = :priority ORDER BY p.documentedDate DESC")
    List<PatientProblem> findProblemsByPatientAndPriority(
        @Param("patientId") UUID patientId, 
        @Param("priority") PatientProblem.Priority priority);
    
    @Query("SELECT p FROM PatientProblem p WHERE p.patient.patientId = :patientId " +
           "AND (LOWER(p.problemName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR p.icd10Code LIKE CONCAT('%', :searchTerm, '%') " +
           "OR p.icd11Code LIKE CONCAT('%', :searchTerm, '%') " +
           "OR p.snomedCode LIKE CONCAT('%', :searchTerm, '%')) " +
           "ORDER BY p.documentedDate DESC")
    List<PatientProblem> searchProblems(@Param("patientId") UUID patientId, 
                                        @Param("searchTerm") String searchTerm);
    
    @Query("SELECT p FROM PatientProblem p WHERE p.patient.patientId = :patientId " +
           "AND p.onsetDate >= :startDate ORDER BY p.onsetDate DESC")
    List<PatientProblem> findProblemsByOnsetDate(@Param("patientId") UUID patientId, 
                                                  @Param("startDate") LocalDate startDate);
    
    @Query("SELECT p FROM PatientProblem p WHERE p.patient.patientId = :patientId " +
           "AND p.resolvedDate >= :startDate AND p.resolvedDate <= :endDate " +
           "ORDER BY p.resolvedDate DESC")
    List<PatientProblem> findResolvedProblemsByDateRange(
        @Param("patientId") UUID patientId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate);
    
    @Query("SELECT COUNT(p) FROM PatientProblem p WHERE p.patient.patientId = :patientId " +
           "AND p.status = 'ACTIVE'")
    Long countActiveProblemsByPatient(@Param("patientId") UUID patientId);
    
    @Query("SELECT COUNT(p) FROM PatientProblem p WHERE p.patient.patientId = :patientId " +
           "AND p.status = 'RESOLVED'")
    Long countResolvedProblemsByPatient(@Param("patientId") UUID patientId);
    
    void deleteByPatientPatientId(UUID patientId);
}
