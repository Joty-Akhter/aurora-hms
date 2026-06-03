package com.easyops.hospital.repository;

import com.easyops.hospital.entity.ImagingStudyHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ImagingStudyHistoryRepository extends JpaRepository<ImagingStudyHistory, UUID> {
    
    List<ImagingStudyHistory> findByStudyStudyId(UUID studyId);
    
    List<ImagingStudyHistory> findByStudyStudyIdOrderByChangedDateDesc(UUID studyId);
    
    @Query("SELECT h FROM ImagingStudyHistory h WHERE h.study.studyId = :studyId " +
           "AND h.changeType = :changeType ORDER BY h.changedDate DESC")
    List<ImagingStudyHistory> findByStudyAndChangeType(
        @Param("studyId") UUID studyId,
        @Param("changeType") ImagingStudyHistory.ChangeType changeType);
    
    @Query("SELECT h FROM ImagingStudyHistory h WHERE h.changedBy = :userId " +
           "ORDER BY h.changedDate DESC")
    List<ImagingStudyHistory> findByChangedBy(@Param("userId") UUID userId);
}
