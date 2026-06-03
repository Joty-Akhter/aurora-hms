package com.easyops.hospital.repository;

import com.easyops.hospital.entity.LabResultProblem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LabResultProblemRepository extends JpaRepository<LabResultProblem, UUID> {
    
    List<LabResultProblem> findByResultResultId(UUID resultId);
    
    List<LabResultProblem> findByProblemProblemId(UUID problemId);
    
    @Query("SELECT lrp FROM LabResultProblem lrp WHERE lrp.result.resultId = :resultId AND lrp.problem.problemId = :problemId")
    LabResultProblem findByResultIdAndProblemId(@Param("resultId") UUID resultId, @Param("problemId") UUID problemId);
    
    @Query("SELECT lrp FROM LabResultProblem lrp WHERE lrp.organizationId = :organizationId")
    List<LabResultProblem> findByOrganizationId(@Param("organizationId") UUID organizationId);
}
