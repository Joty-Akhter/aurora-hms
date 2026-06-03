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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service for DICOM image storage and file management.
 * Handles DICOM Part 10 file format storage, retrieval, and management.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DICOMImageStorageService {
    
    private final ImagingImageAttachmentRepository imageAttachmentRepository;
    private final ImagingStudyRepository imagingStudyRepository;
    @Value("${dicom.storage.base-path:./storage/dicom}")
    private String baseStoragePath;
    
    @Value("${dicom.storage.enabled:true}")
    private boolean storageEnabled;
    
    /**
     * Store a DICOM file (Part 10 format)
     */
    @Transactional
    public ImagingImageAttachment storeDicomFile(UUID studyId, MultipartFile file, UUID uploadedBy) throws IOException {
        log.info("Storing DICOM file for study: {}", studyId);
        
        if (!storageEnabled) {
            throw new IllegalStateException("DICOM storage is disabled");
        }
        
        ImagingStudy study = imagingStudyRepository.findById(studyId)
            .orElseThrow(() -> new RuntimeException("Imaging study not found: " + studyId));

        // Single buffer: one parse (Part 10 FMI + dataset) and one disk write — avoids broken multipart re-reads.
        byte[] content = file.getBytes();
        Attributes attributes = readDicomAttributes(content);
        String sopInstanceUid = attributes.getString(Tag.SOPInstanceUID);
        String seriesInstanceUid = attributes.getString(Tag.SeriesInstanceUID);
        
        // Check if SOP Instance UID already exists
        if (sopInstanceUid != null && imageAttachmentRepository.findByDicomSopInstanceUid(sopInstanceUid).isPresent()) {
            throw new RuntimeException("DICOM file with SOP Instance UID already exists: " + sopInstanceUid);
        }
        
        // Generate file path
        String fileName = generateFileName(studyId, sopInstanceUid, file.getOriginalFilename());
        Path storagePath = Paths.get(baseStoragePath, studyId.toString());
        Files.createDirectories(storagePath);
        
        Path filePath = storagePath.resolve(fileName);

        Files.write(filePath, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        
        // Create database record
        ImagingImageAttachment attachment = ImagingImageAttachment.builder()
            .study(study)
            .fileName(fileName)
            .fileType(file.getContentType() != null ? file.getContentType() : "application/dicom")
            .fileSize(file.getSize())
            .filePath(filePath.toString())
            .fileUrl(null)
            .imageType(ImagingImageAttachment.ImageType.DICOM)
            .isDicom(true)
            .dicomSeriesInstanceUid(seriesInstanceUid)
            .dicomSopInstanceUid(sopInstanceUid)
            .uploadedBy(uploadedBy)
            .uploadedDate(LocalDateTime.now())
            .build();
        
        ImagingImageAttachment savedAttachment = imageAttachmentRepository.save(attachment);
        savedAttachment.setFileUrl("/api/hospital/dicom/images/" + savedAttachment.getAttachmentId() + "/download");
        savedAttachment = imageAttachmentRepository.save(savedAttachment);
        
        // Update study DICOM information if not set
        if (study.getDicomStudyInstanceUid() == null) {
            String studyInstanceUid = attributes.getString(Tag.StudyInstanceUID);
            study.setDicomStudyInstanceUid(studyInstanceUid);
            study.setDicomSeriesInstanceUid(seriesInstanceUid);
            study.setDicomStorageLocation(storagePath.toString());
            study.setImagesAvailable(true);
            imagingStudyRepository.save(study);
        }
        
        log.info("Stored DICOM file: {} for study: {}", fileName, studyId);
        return savedAttachment;
    }
    
    /**
     * Retrieve DICOM file
     */
    public File retrieveDicomFile(UUID attachmentId) throws FileNotFoundException {
        ImagingImageAttachment attachment = imageAttachmentRepository.findById(attachmentId)
            .orElseThrow(() -> new RuntimeException("Image attachment not found: " + attachmentId));
        
        File file = new File(attachment.getFilePath());
        if (!file.exists() || !file.isFile() || !file.canRead()) {
            throw new FileNotFoundException("DICOM file not found or not readable: " + attachment.getFilePath());
        }

        return file;
    }
    
    /**
     * Delete DICOM file
     */
    @Transactional
    public void deleteDicomFile(UUID attachmentId) throws IOException {
        ImagingImageAttachment attachment = imageAttachmentRepository.findById(attachmentId)
            .orElseThrow(() -> new RuntimeException("Image attachment not found: " + attachmentId));
        
        // Delete physical file
        Path filePath = Paths.get(attachment.getFilePath());
        if (Files.exists(filePath)) {
            Files.delete(filePath);
        }
        
        // Delete thumbnail if exists
        if (attachment.getThumbnailPath() != null) {
            Path thumbnailPath = Paths.get(attachment.getThumbnailPath());
            if (Files.exists(thumbnailPath)) {
                Files.delete(thumbnailPath);
            }
        }
        
        // Delete database record
        imageAttachmentRepository.delete(attachment);
        
        log.info("Deleted DICOM file: {}", attachmentId);
    }
    
    /**
     * Get all DICOM images for a study
     */
    public List<ImagingImageAttachment> getDicomImagesByStudy(UUID studyId) {
        return imageAttachmentRepository.findDicomImagesByStudyId(studyId);
    }
    
    /**
     * Get DICOM file by SOP Instance UID
     */
    public ImagingImageAttachment getDicomFileBySopInstanceUid(String sopInstanceUid) {
        return imageAttachmentRepository.findByDicomSopInstanceUid(sopInstanceUid)
            .orElseThrow(() -> new RuntimeException("DICOM file not found with SOP Instance UID: " + sopInstanceUid));
    }
    
    /**
     * Get all DICOM files in a series
     */
    public List<ImagingImageAttachment> getDicomFilesBySeries(String seriesInstanceUid) {
        return imageAttachmentRepository.findByDicomSeriesInstanceUidOrdered(seriesInstanceUid);
    }
    
    /**
     * Parses a DICOM Part 10 upload by loading it into memory, then applying the same pipeline as
     * {@link #readDicomAttributes(byte[])} (file meta information group 0002,xxxx first, then the dataset).
     * <p>
     * Prefer {@link #readDicomAttributes(byte[])} when you already have the bytes (e.g. single {@code getBytes()}
     * for both parse and disk write) to avoid an extra buffer copy.
     */
    private Attributes readDicomAttributes(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Upload file is required");
        }
        return readDicomAttributes(file.getBytes());
    }

    /**
     * Part 10 / DICOM File Format: {@link DicomInputStream#readFileMetaInformation()} must run before
     * {@link DicomInputStream#readDataset()} so the 128-byte preamble and meta information group (0002,xxxx)
     * are consumed and the following bytes are interpreted as the correct dataset transfer syntax.
     * Validates SOP Class UID and SOP Instance UID from FMI and/or dataset.
     *
     * @return the parsed <strong>dataset</strong> attributes (not the file meta information object)
     */
    private Attributes readDicomAttributes(byte[] content) throws IOException {
        try (DicomInputStream dis = new DicomInputStream(new ByteArrayInputStream(content))) {
            Attributes fmi = dis.readFileMetaInformation();
            Attributes ds = dis.readDataset();
            String sopClassUid = null;
            String sopInstanceUid = null;
            if (fmi != null && !fmi.isEmpty()) {
                sopClassUid = fmi.getString(Tag.MediaStorageSOPClassUID);
                sopInstanceUid = fmi.getString(Tag.MediaStorageSOPInstanceUID);
            }
            if (sopClassUid == null || sopClassUid.isBlank()) {
                sopClassUid = ds.getString(Tag.SOPClassUID);
            }
            if (sopInstanceUid == null || sopInstanceUid.isBlank()) {
                sopInstanceUid = ds.getString(Tag.SOPInstanceUID);
            }
            if (sopClassUid == null || sopClassUid.isBlank() || sopInstanceUid == null || sopInstanceUid.isBlank()) {
                throw new IllegalArgumentException("File is not a valid DICOM file");
            }
            return ds;
        }
    }

    /**
     * Generate file name for DICOM file
     */
    private String generateFileName(UUID studyId, String sopInstanceUid, String originalFileName) {
        if (sopInstanceUid != null) {
            return sopInstanceUid.replace(".", "_") + ".dcm";
        }
        if (originalFileName != null && originalFileName.endsWith(".dcm")) {
            return originalFileName;
        }
        return studyId.toString() + "_" + System.currentTimeMillis() + ".dcm";
    }
}
