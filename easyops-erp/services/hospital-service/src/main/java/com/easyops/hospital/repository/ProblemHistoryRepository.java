package com.easyops.hospital.repository;

import com.easyops.hospital.entity.ProblemHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProblemHistoryRepository extends JpaRepository<ProblemHistory, UUID> {
    
    List<ProblemHistory> findByProblemProblemId(UUID problemId);
    
    List<ProblemHistory> findByProblemProblemIdOrderByChangedDateDesc(UUID problemId);
    
    List<ProblemHistory> findByProblemProblemIdAndChangeType(
        UUID problemId, ProblemHistory.ChangeType changeType);
    
    @Query("SELECT h FROM ProblemHistory h WHERE h.problem.problemId = :problemId " +
           "ORDER BY h.changedDate DESC")
    List<ProblemHistory> findHistoryByProblem(@Param("problemId") UUID problemId);
    
    @Query("SELECT h FROM ProblemHistory h WHERE h.problem.patient.patientId = :patientId " +
           "ORDER BY h.changedDate DESC")
    List<ProblemHistory> findHistoryByPatient(@Param("patientId") UUID patientId);
    
    void deleteByProblemProblemId(UUID problemId);
}
