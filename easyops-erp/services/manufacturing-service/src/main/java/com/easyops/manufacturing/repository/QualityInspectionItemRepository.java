package com.easyops.manufacturing.repository;

import com.easyops.manufacturing.entity.QualityInspectionItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface QualityInspectionItemRepository extends JpaRepository<QualityInspectionItem, UUID> {

    @Query("SELECT qii FROM QualityInspectionItem qii WHERE qii.qualityInspection.inspectionId = :inspectionId " +
           "ORDER BY qii.sequenceNumber")
    List<QualityInspectionItem> findByInspectionId(@Param("inspectionId") UUID inspectionId);

    @Query("SELECT qii FROM QualityInspectionItem qii WHERE qii.qualityInspection.inspectionId = :inspectionId " +
           "AND qii.passFail = :passFail")
    List<QualityInspectionItem> findByInspectionIdAndPassFail(@Param("inspectionId") UUID inspectionId, 
                                                               @Param("passFail") String passFail);

    @Query("SELECT qii FROM QualityInspectionItem qii WHERE qii.qualityInspection.inspectionId = :inspectionId " +
           "AND qii.isCritical = true")
    List<QualityInspectionItem> findCriticalParametersByInspectionId(@Param("inspectionId") UUID inspectionId);

    @Query("SELECT COUNT(qii) FROM QualityInspectionItem qii WHERE qii.qualityInspection.inspectionId = :inspectionId")
    long countByInspectionId(@Param("inspectionId") UUID inspectionId);

    @Query("SELECT COUNT(qii) FROM QualityInspectionItem qii WHERE qii.qualityInspection.inspectionId = :inspectionId " +
           "AND qii.passFail = 'FAIL'")
    long countFailedParameters(@Param("inspectionId") UUID inspectionId);
}

