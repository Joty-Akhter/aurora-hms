package com.easyops.hospital.repository;

import com.easyops.hospital.entity.LabResultHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LabResultHistoryRepository extends JpaRepository<LabResultHistory, UUID> {
    
    List<LabResultHistory> findByResultResultId(UUID resultId);
    
    List<LabResultHistory> findByResultResultIdOrderByChangedDateDesc(UUID resultId);
    
    @Query("SELECT h FROM LabResultHistory h WHERE h.result.resultId = :resultId " +
           "AND h.changeType = :changeType ORDER BY h.changedDate DESC")
    List<LabResultHistory> findByResultAndChangeType(
        @Param("resultId") UUID resultId,
        @Param("changeType") LabResultHistory.ChangeType changeType);
    
    @Query("SELECT h FROM LabResultHistory h WHERE h.changedBy = :userId " +
           "ORDER BY h.changedDate DESC")
    List<LabResultHistory> findByChangedBy(@Param("userId") UUID userId);
}
