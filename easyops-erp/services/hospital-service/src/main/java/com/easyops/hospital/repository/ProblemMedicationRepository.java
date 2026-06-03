package com.easyops.hospital.repository;

import com.easyops.hospital.entity.ProblemMedication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProblemMedicationRepository extends JpaRepository<ProblemMedication, UUID> {
    
    List<ProblemMedication> findByProblemProblemId(UUID problemId);
    
    List<ProblemMedication> findByMedicationMedicationId(UUID medicationId);
    
    @Query("SELECT pm FROM ProblemMedication pm WHERE pm.problem.problemId = :problemId " +
           "ORDER BY pm.linkedDate DESC")
    List<ProblemMedication> findByProblemIdOrdered(@Param("problemId") UUID problemId);
    
    @Query("SELECT pm FROM ProblemMedication pm WHERE pm.medication.medicationId = :medicationId " +
           "ORDER BY pm.linkedDate DESC")
    List<ProblemMedication> findByMedicationIdOrdered(@Param("medicationId") UUID medicationId);
    
    @Query("SELECT pm FROM ProblemMedication pm WHERE pm.problem.problemId = :problemId AND pm.medication.medicationId = :medicationId")
    ProblemMedication findByProblemIdAndMedicationId(@Param("problemId") UUID problemId, @Param("medicationId") UUID medicationId);
    
    void deleteByProblemProblemId(UUID problemId);
    
    void deleteByMedicationMedicationId(UUID medicationId);
}
