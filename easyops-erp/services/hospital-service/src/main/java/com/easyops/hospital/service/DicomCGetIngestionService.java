package com.easyops.hospital.service;

import com.easyops.hospital.entity.ImagingImageAttachment;
import com.easyops.hospital.entity.ImagingStudy;
import com.easyops.hospital.repository.ImagingImageAttachmentRepository;
import com.easyops.hospital.repository.ImagingStudyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.io.DicomInputStream;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Persists DICOM instances received via C-GET into {@link ImagingImageAttachment} records.
 * Runs in a dedicated transactional boundary (callbacks from dcm4che must not rely on self-invocation).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DicomCGetIngestionService {

    private static final UUID SYSTEM_NETWORK_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    private final ImagingStudyRepository imagingStudyRepository;
    private final ImagingImageAttachmentRepository imageAttachmentRepository;
    private final DICOMThumbnailService dicomThumbnailService;

    public enum Outcome {
        IMPORTED,
        SKIPPED_DUPLICATE,
        SKIPPED_NO_MATCHING_STUDY
    }

    @Transactional
    public Outcome ingestReceivedInstance(File receivedFile, String requestedStudyInstanceUid, String sopInstanceUid) {
        if (sopInstanceUid == null || sopInstanceUid.isBlank()) {
            throw new IllegalArgumentException("SOP Instance UID is required for C-GET ingest");
        }
        if (requestedStudyInstanceUid == null || requestedStudyInstanceUid.isBlank()) {
            throw new IllegalArgumentException("StudyInstanceUID is required for C-GET ingest");
        }
        if (receivedFile == null || !receivedFile.isFile() || !receivedFile.canRead()) {
            throw new IllegalArgumentException("Received DICOM file is missing or not readable");
        }
        if (imageAttachmentRepository.findByDicomSopInstanceUid(sopInstanceUid).isPresent()) {
            return Outcome.SKIPPED_DUPLICATE;
        }

        String seriesUid = null;
        String studyUidFromFile = null;
        try (DicomInputStream dis = new DicomInputStream(receivedFile)) {
            dis.readFileMetaInformation();
            Attributes ds = dis.readDataset();
            seriesUid = ds.getString(Tag.SeriesInstanceUID);
            studyUidFromFile = ds.getString(Tag.StudyInstanceUID);
        } catch (Exception e) {
            log.debug("Could not read dataset from C-GET file {}", receivedFile.getName(), e);
        }

        Optional<ImagingStudy> studyOpt =
            imagingStudyRepository.findByDicomStudyInstanceUid(requestedStudyInstanceUid.trim());
        if (studyOpt.isEmpty() && studyUidFromFile != null && !studyUidFromFile.isBlank()) {
            studyOpt = imagingStudyRepository.findByDicomStudyInstanceUid(studyUidFromFile.trim());
            if (studyOpt.isPresent() && !requestedStudyInstanceUid.trim().equals(studyUidFromFile.trim())) {
                log.info(
                    "C-GET instance {} matched study by file StudyInstanceUID (differs from request)",
                    sopInstanceUid);
            }
        }

        if (studyOpt.isEmpty()) {
            log.debug(
                "C-GET received instance {}, no ImagingStudy for StudyInstanceUID {} / {}",
                sopInstanceUid,
                requestedStudyInstanceUid,
                studyUidFromFile);
            return Outcome.SKIPPED_NO_MATCHING_STUDY;
        }

        ImagingStudy study = studyOpt.get();

        ImagingImageAttachment attachment = ImagingImageAttachment.builder()
            .study(study)
            .fileName(receivedFile.getName())
            .fileType("application/dicom")
            .fileSize(receivedFile.length())
            .filePath(receivedFile.getAbsolutePath())
            .fileUrl(null)
            .imageType(ImagingImageAttachment.ImageType.DICOM)
            .isDicom(true)
            .dicomSeriesInstanceUid(seriesUid)
            .dicomSopInstanceUid(sopInstanceUid)
            .uploadedBy(SYSTEM_NETWORK_USER_ID)
            .uploadedDate(LocalDateTime.now())
            .build();
        try {
            ImagingImageAttachment saved = imageAttachmentRepository.save(attachment);
            saved.setFileUrl("/api/hospital/dicom/images/" + saved.getAttachmentId() + "/download");

            try {
                File thumbnail = dicomThumbnailService.generateThumbnail(
                    receivedFile,
                    study.getStudyId(),
                    sopInstanceUid);
                if (thumbnail != null) {
                    saved.setThumbnailPath(thumbnail.getAbsolutePath());
                    saved.setThumbnailUrl(dicomThumbnailService.getThumbnailUrl(saved.getAttachmentId()));
                }
            } catch (Exception e) {
                log.debug("Thumbnail generation skipped for C-GET received instance {}", sopInstanceUid, e);
            }
            imageAttachmentRepository.save(saved);
            return Outcome.IMPORTED;
        } catch (DataIntegrityViolationException e) {
            log.debug("C-GET ingest duplicate SOP {} (concurrent insert)", sopInstanceUid);
            return Outcome.SKIPPED_DUPLICATE;
        }
    }
}
