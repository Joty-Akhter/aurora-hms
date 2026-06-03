package com.easyops.hospital.repository;

import com.easyops.hospital.entity.ImagingStudyProblem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ImagingStudyProblemRepository extends JpaRepository<ImagingStudyProblem, UUID> {
    
    List<ImagingStudyProblem> findByStudyStudyId(UUID studyId);
    
    List<ImagingStudyProblem> findByProblemProblemId(UUID problemId);
    
    @Query("SELECT l FROM ImagingStudyProblem l WHERE l.study.studyId = :studyId " +
           "ORDER BY l.linkedDate DESC")
    List<ImagingStudyProblem> findByStudyIdOrdered(@Param("studyId") UUID studyId);
    
    @Query("SELECT l FROM ImagingStudyProblem l WHERE l.problem.problemId = :problemId " +
           "ORDER BY l.linkedDate DESC")
    List<ImagingStudyProblem> findByProblemIdOrdered(@Param("problemId") UUID problemId);
    
    void deleteByStudyStudyId(UUID studyId);
    
    void deleteByProblemProblemId(UUID problemId);
}
