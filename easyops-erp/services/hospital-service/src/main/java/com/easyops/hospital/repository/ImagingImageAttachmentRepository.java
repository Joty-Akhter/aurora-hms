package com.easyops.hospital.repository;

import com.easyops.hospital.entity.ImagingImageAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ImagingImageAttachmentRepository extends JpaRepository<ImagingImageAttachment, UUID> {
    
    List<ImagingImageAttachment> findByStudyStudyId(UUID studyId);
    
    List<ImagingImageAttachment> findByStudyStudyIdOrderByUploadedDateDesc(UUID studyId);
    
    @Query("SELECT a FROM ImagingImageAttachment a WHERE a.study.studyId = :studyId AND a.isDicom = true")
    List<ImagingImageAttachment> findDicomImagesByStudyId(@Param("studyId") UUID studyId);
    
    @Query("SELECT a FROM ImagingImageAttachment a WHERE a.study.studyId = :studyId AND a.isDicom = false")
    List<ImagingImageAttachment> findNonDicomImagesByStudyId(@Param("studyId") UUID studyId);
    
    Optional<ImagingImageAttachment> findByDicomSopInstanceUid(String sopInstanceUid);
    
    Optional<ImagingImageAttachment> findByDicomSeriesInstanceUid(String seriesInstanceUid);
    
    @Query("SELECT a FROM ImagingImageAttachment a WHERE a.dicomSeriesInstanceUid = :seriesInstanceUid ORDER BY a.uploadedDate")
    List<ImagingImageAttachment> findByDicomSeriesInstanceUidOrdered(@Param("seriesInstanceUid") String seriesInstanceUid);
    
    void deleteByStudyStudyId(UUID studyId);
}
